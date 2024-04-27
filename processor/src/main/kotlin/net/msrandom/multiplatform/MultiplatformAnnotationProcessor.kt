package net.msrandom.multiplatform

import com.sun.source.tree.ImportTree
import com.sun.source.tree.Tree
import com.sun.tools.javac.api.JavacTrees
import com.sun.tools.javac.processing.JavacProcessingEnvironment
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit
import com.sun.tools.javac.tree.JCTree.JCVariableDecl
import com.sun.tools.javac.util.Context
import net.msrandom.multiplatform.annotations.Actual
import net.msrandom.multiplatform.annotations.Expect
import net.msrandom.multiplatform.bootstrap.PlatformHelper
import javax.annotation.processing.*
import javax.lang.model.element.*

private val EXPECT_ANNOTATION_NAME = Expect::class.simpleName!!
private val ACTUAL_ANNOTATION_NAME = Actual::class.simpleName!!

private inline fun <T> getActual(implementations: List<T>, name: () -> CharSequence) = when {
    implementations.isEmpty() -> throw IllegalArgumentException("${name()} includes @$EXPECT_ANNOTATION_NAME with no @$ACTUAL_ANNOTATION_NAME")
    implementations.size > 1 -> throw UnsupportedOperationException("${name()} includes @$EXPECT_ANNOTATION_NAME has more than one @$ACTUAL_ANNOTATION_NAME")
    else -> implementations.first()
}

// Loaded reflectively after com.sun.tools.javac packages are exported
@Suppress("unused")
class MultiplatformAnnotationProcessor(
    private val processingEnvironment: ProcessingEnvironment,
    private val platformHelper: PlatformHelper,
    private val generateStubs: Boolean
) {
    private val context: Context
    private val trees: JavacTrees

    init {
        processingEnvironment as JavacProcessingEnvironment

        context = processingEnvironment.context
        trees = JavacTrees.instance(context)
    }

    fun process(roundEnvironment: RoundEnvironment) {
        val allExpected = roundEnvironment.getElementsAnnotatedWith(Expect::class.java)
        val allActual = roundEnvironment.getElementsAnnotatedWith(Actual::class.java)

        val implementations = allExpected.associateWith { expect ->
            if (expect is TypeElement) {
                // class, enum, record, interface, annotation

                val implementations = allActual.filter { actual ->
                    actual is TypeElement && actual.qualifiedName contentEquals expect.qualifiedName.toString() + ACTUAL_ANNOTATION_NAME
                }

                val actual = getActual(implementations, expect::getQualifiedName)

                if (actual.kind != expect.kind) {
                    throw UnsupportedOperationException("@$ACTUAL_ANNOTATION_NAME type is a different kind from its @$EXPECT_ANNOTATION_NAME, expected ${expect.kind} but found ${actual.kind}")
                }

                if (actual.modifiers != expect.modifiers) {
                    throw UnsupportedOperationException("@$ACTUAL_ANNOTATION_NAME type has different modifiers from its @$EXPECT_ANNOTATION_NAME, expected ${expect.modifiers} but found ${actual.modifiers}")
                }

                actual
            } else if (expect is ExecutableElement && expect.kind == ElementKind.METHOD) {
                val expectOwner = expect.enclosingElement as TypeElement
                val expectOwnerName = expectOwner.qualifiedName

                val implementations = allActual.filterIsInstance<ExecutableElement>().filter { actual ->
                    val actualOwner = actual.enclosingElement as TypeElement
                    val actualOwnerName = actualOwner.qualifiedName

                    if (!actualOwnerName.endsWith(ACTUAL_ANNOTATION_NAME)) {
                        throw IllegalArgumentException("$actualOwnerName does not end with $ACTUAL_ANNOTATION_NAME")
                    }

                    actual.kind == ElementKind.METHOD &&
                            actualOwnerName contentEquals expectOwnerName.toString() + ACTUAL_ANNOTATION_NAME &&
                            actual.simpleName == expect.simpleName &&
                            actual.parameters.map(VariableElement::asType) == expect.parameters.map(VariableElement::asType)
                }

                getActual(implementations) {
                    "$expectOwnerName.${expect.simpleName}"
                }
            } else if (expect is VariableElement && expect.kind == ElementKind.FIELD) {
                val expectOwner = expect.enclosingElement as TypeElement
                val expectOwnerName = expectOwner.qualifiedName

                val implementations = allActual.filterIsInstance<VariableElement>().filter { actual ->
                    val actualOwner = actual.enclosingElement as TypeElement
                    val actualOwnerName = actualOwner.qualifiedName

                    if (!actualOwnerName.endsWith(ACTUAL_ANNOTATION_NAME)) {
                        throw IllegalArgumentException("$actualOwnerName does not end with $ACTUAL_ANNOTATION_NAME")
                    }

                    actual.kind == ElementKind.FIELD &&
                            actualOwnerName contentEquals expectOwnerName.toString() + ACTUAL_ANNOTATION_NAME &&
                            actual.simpleName == expect.simpleName
                }

                val actual = getActual(implementations) {
                    "$expectOwnerName.${expect.simpleName}"
                }

                if (actual.asType() != expect.asType()) {
                    throw UnsupportedOperationException("@$EXPECT_ANNOTATION_NAME field has differing type from @$ACTUAL_ANNOTATION_NAME, expected ${expect.asType()} but found ${actual.asType()}")
                }

                actual
            } else {
                throw UnsupportedOperationException("Found @$EXPECT_ANNOTATION_NAME on element of unsupported kind ${expect.kind}")
            }
        }

        allActual.firstOrNull {
            it !in implementations.values &&
                    (it.enclosingElement !is TypeElement || it.enclosingElement.getAnnotation(Actual::class.java) == null)
        }?.let {
            val ownerName = (it.enclosingElement as TypeElement).qualifiedName

            throw IllegalArgumentException("$ownerName.${it.simpleName} includes an @$ACTUAL_ANNOTATION_NAME without a corresponding @$EXPECT_ANNOTATION_NAME")
        }

        fun clearActual(unit: JCCompilationUnit) {
            // We don't want to keep the @Actual files
            unit.defs = JavaCompilerList.nil()
        }

        fun clearActualParent(element: Element) {
            val unit = trees.getPath(element.enclosingElement)?.compilationUnit as? JCCompilationUnit

            unit?.let(::clearActual)
        }

        for ((expect, actual) in implementations.entries) {
            when (expect) {
                is ExecutableElement -> {
                    require(actual is ExecutableElement)

                    val expectTree = trees.getTree(expect)
                    val actualTree = trees.getTree(actual)

                    if (expectTree.getBody() != null) {
                        throw UnsupportedOperationException("@$EXPECT_ANNOTATION_NAME method ${(expect.enclosingElement as TypeElement).qualifiedName}.${expect.simpleName} has body")
                    }

                    expectTree.body = actualTree.getBody()

                    clearActualParent(actual)
                }

                is VariableElement -> {
                    require(actual is VariableElement)

                    val expectTree = trees.getTree(expect) as JCVariableDecl
                    val actualTree = trees.getTree(actual) as JCVariableDecl

                    if (expectTree.initializer != null) {
                        throw UnsupportedOperationException("@$EXPECT_ANNOTATION_NAME field ${(expect.enclosingElement as TypeElement).qualifiedName}.${expect.simpleName} has initializer")
                    }

                    expectTree.init = actualTree.initializer

                    clearActualParent(actual)
                }

                is TypeElement -> {
                    require(actual is TypeElement)

                    val expectTree = trees.getTree(expect)
                    val actualTree = trees.getTree(actual)

                    val expectCompilationUnit = trees.getPath(expect).compilationUnit as JCCompilationUnit
                    val actualCompilationUnit = trees.getPath(actual).compilationUnit as JCCompilationUnit

                    expectTree.prepareClass(processingEnvironment, platformHelper.elementRemover)

                    val members = actualTree.members.map {
                        it.clone(context)
                    }

                    val imports = (expectCompilationUnit.imports + actualCompilationUnit.imports).filterNot {
                        it.kind == Tree.Kind.IMPORT && (it as ImportTree).qualifiedIdentifier.toString().startsWith(Expect::class.java.getPackage().name)
                    }

                    val unitDefs =
                        (listOf(expectCompilationUnit.defs.head) + imports + expectCompilationUnit.typeDecls)

                    expectCompilationUnit.defs = JavaCompilerList.from(unitDefs)

                    clearActual(actualCompilationUnit)

                    expectTree.pos = actualTree.pos
                    expectTree.mods.pos = actualTree.mods.pos

                    expectTree.mods.annotations.firstOrNull { Expect::class.qualifiedName.equals(it.type?.toString()) }?.let {
                        expectTree.mods.annotations = JavaCompilerList.filter(expectTree.mods.annotations, it)
                    }

                    expectTree.typarams = actualTree.typeParameters
                    expectTree.extending = actualTree.extendsClause
                    expectTree.implementing = actualTree.implementsClause
                    expectTree.defs = JavaCompilerList.from(members)

                    expectCompilationUnit.finalizeClass(context)
                }
            }
        }
    }
}

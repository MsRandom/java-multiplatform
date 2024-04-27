package net.msrandom.multiplatform

import com.sun.tools.javac.comp.Enter
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit
import com.sun.tools.javac.tree.JCTree.JCVariableDecl
import com.sun.tools.javac.tree.JCTree.JCMethodDecl
import com.sun.tools.javac.tree.JCTree.JCExpression
import com.sun.tools.javac.tree.JCTree.JCClassDecl
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.List
import net.msrandom.multiplatform.annotations.Actual
import net.msrandom.multiplatform.bootstrap.ElementRemover
import javax.annotation.processing.ProcessingEnvironment

typealias JavaCompilerList<T> = List<T>

fun JCTree.clone(context: Context): JCTree {
    val treeMaker = TreeMaker.instance(context)

    return when (this) {
        is JCMethodDecl -> {
            treeMaker.MethodDef(
                modifiers,
                getName(),
                returnType as JCExpression?,
                typeParameters,
                receiverParameter,
                parameters,
                throws,
                getBody(),
                getDefaultValue() as JCExpression?,
            ).also {
                it.pos = pos
                it.mods.pos = mods.pos

                it.mods.annotations.firstOrNull { annotation -> Actual::class.qualifiedName.equals(annotation.type?.toString()) }?.let { annotation ->
                    it.mods.annotations = JavaCompilerList.filter(it.mods.annotations, annotation)
                }
            }
        }

        is JCVariableDecl -> {
            treeMaker.VarDef(
                modifiers,
                getName(),
                getType() as JCExpression?,
                initializer,
            ).also {
                it.pos = pos
                it.mods.pos = mods.pos

                it.mods.annotations.firstOrNull { annotation -> Actual::class.qualifiedName.equals(annotation.type?.toString()) }?.let { annotation ->
                    it.mods.annotations = JavaCompilerList.filter(it.mods.annotations, annotation)
                }
            }
        }

        else -> {
            throw UnsupportedOperationException("Attempting to clone $kind")
        }
    }
}

fun JCClassDecl.prepareClass(processingEnvironment: ProcessingEnvironment, elementRemover: ElementRemover) {
    elementRemover(processingEnvironment, sym)

    sym = null
}

fun JCCompilationUnit.finalizeClass(context: Context) {
    Enter.instance(context).complete(JavaCompilerList.of(this), null)
}

package net.msrandom.multiplatform.inspection

import com.intellij.psi.*
import net.msrandom.multiplatform.annotations.Actual
import net.msrandom.multiplatform.annotations.Expect

fun create(declaration: PsiClass, elementFactory: PsiElementFactory, actualImport: PsiImportStatement, fileGenerator: (PsiClass) -> PsiClass): PsiClass {
    val actualClass = fileGenerator(declaration)

    (actualClass.containingFile as PsiJavaFile).importList?.add(actualImport)

    actualClass.typeParameterList!!.replace(declaration.typeParameterList!!)

    actualClass.modifierList!!.replace(elementFactory.createFieldFromText("int dummy", null).modifierList!!)
    for (child in declaration.modifierList!!.children) {
        if (child is PsiAnnotation && child.hasQualifiedName(Expect::class.qualifiedName!!)) {
            continue
        }

        actualClass.modifierList!!.add(child)
    }

    actualClass.modifierList!!.addAnnotation(Actual::class.simpleName!!)

    for (field in declaration.fields) {
        create(field, elementFactory, fileGenerator)
    }

    for (method in declaration.methods) {
        create(method, elementFactory, fileGenerator)
    }

    return actualClass
}

fun create(declaration: PsiMethod, elementFactory: PsiElementFactory, fileGenerator: (PsiClass) -> PsiClass): PsiClass {
    val actualClass = fileGenerator(declaration.containingClass!!)

    val isConstructor = declaration.name == declaration.containingClass!!.name

    val method = if (isConstructor) {
        elementFactory.createConstructor(declaration.name)
    } else {
        elementFactory.createMethod(declaration.name, declaration.returnType)
    }

    method.parameterList.replace(declaration.parameterList)
    method.typeParameterList!!.replace(declaration.typeParameterList!!)

    method.modifierList.replace(elementFactory.createFieldFromText("int dummy", null).modifierList!!)
    for (child in declaration.modifierList.children) {
        if (child is PsiAnnotation && child.hasQualifiedName(Expect::class.qualifiedName!!)) {
            continue
        }

        method.modifierList.add(child)
    }

    method.modifierList.addAnnotation(Actual::class.simpleName!!)

    actualClass.add(method)

    return actualClass
}

fun create(declaration: PsiField, elementFactory: PsiElementFactory, fileGenerator: (PsiClass) -> PsiClass): PsiClass {
    val actualClass = fileGenerator(declaration.containingClass!!)

    val field = elementFactory.createField(declaration.name, declaration.type)

    field.modifierList!!.replace(elementFactory.createFieldFromText("int dummy", null).modifierList!!)
    for (child in declaration.modifierList!!.children) {
        if (child is PsiAnnotation && child.hasQualifiedName(Expect::class.qualifiedName!!)) {
            continue
        }

        field.modifierList!!.add(child)
    }

    field.modifierList!!.addAnnotation(Actual::class.simpleName!!)

    actualClass.add(field)

    return actualClass
}

package net.msrandom.multiplatform

import com.intellij.lang.jvm.JvmParameter
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import net.msrandom.multiplatform.annotations.Actual
import net.msrandom.multiplatform.annotations.Expect

val ACTUAL_SUFFIX = Actual::class.simpleName!!

val PsiModifierListOwner.isExpect
    get() = hasAnnotation(Expect::class.qualifiedName!!)

val PsiModifierListOwner.isActual
    get() = hasAnnotation(Actual::class.qualifiedName!!)

val PsiClass.actuals: Array<PsiClass>
    get() = JavaPsiFacade
        .getInstance(project)
        .findClasses(
            qualifiedName + ACTUAL_SUFFIX,
            GlobalSearchScope.projectScope(project)
        )

val PsiClass.expect: PsiClass?
    get() = JavaPsiFacade
        .getInstance(project)
        .findClass(
            qualifiedName!!.removeSuffix(ACTUAL_SUFFIX),
            GlobalSearchScope.projectScope(project)
        )

fun PsiField.actual(actual: PsiClass) = actual.fields.firstOrNull { field ->
    field.name == name && field.isActual
}

fun PsiMethod.actual(actual: PsiClass) = actual.methods.firstOrNull { method ->
    val constructor = name == containingClass!!.name

    val nameMatches = if (constructor) {
        method.name == method.containingClass!!.name
    } else {
        method.name == name
    }

    nameMatches &&
            method.parameters.map(JvmParameter::getType) == parameters.map(JvmParameter::getType) &&
            method.typeParameters.size == typeParameters.size &&
            method.typeParameters.zip(typeParameters).map { (a, b) -> a.bounds contentEquals b.bounds }.all { it } &&
            method.isActual
}

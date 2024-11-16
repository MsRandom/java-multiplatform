package net.msrandom.multiplatform.lines

import com.intellij.codeInsight.daemon.DefaultGutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.icons.AllIcons
import com.intellij.lang.jvm.JvmParameter
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.*
import net.msrandom.multiplatform.*
import net.msrandom.multiplatform.annotations.Actual

class ActualLineMarkerProviderDescriptor : LineMarkerProviderDescriptor() {
    override fun getName() =
        MultiplatformInsightBundle.message("multiplatform.actual.find-expect")

    private fun handle(element: PsiMethod): LineMarkerInfo<*>? {
        if (!element.isActual) {
            return null
        }

        val owner = element.containingClass!!
        val expectClass = owner.expect ?: return null
        val ownerExpect = expectClass.isExpect

        fun isExpect(element: PsiModifierListOwner) =
            ownerExpect || element.isExpect

        val tooltip = MultiplatformInsightBundle.message("multiplatform.actual.implements-expect", "<b>${element.name}</b>")

        val constructor = element.name == owner.name

        val implementations = owner.expect?.methods
            ?.filter { method ->
                val nameMatches = if (constructor) {
                    method.name == method.containingClass!!.name
                } else {
                    method.name == element.name
                }

                nameMatches &&
                        method.parameters.map { it.type.toString() } == element.parameters.map { it.type.toString() } &&
                        method.typeParameters.size == element.typeParameters.size &&
                        method.typeParameters.zip(element.typeParameters).map { (a, b) -> a.bounds contentEquals b.bounds }.all { it } &&
                        isExpect(method)
            }
            ?.takeUnless(List<*>::isEmpty)
            ?: return null

        return LineMarkerInfo(
            element.nameIdentifier!!,
            element.nameIdentifier!!.textRange,
            AllIcons.Gutter.ImplementingMethod,
            { tooltip },
            DefaultGutterIconNavigationHandler(
                implementations,
                tooltip,
            ),
            GutterIconRenderer.Alignment.LEFT,
            { tooltip }
        )
    }

    private fun handle(element: PsiField): LineMarkerInfo<*>? {
        if (!element.isActual) {
            return null
        }

        val owner = element.containingClass!!
        val expectClass = owner.expect ?: return null
        val ownerExpect = expectClass.isExpect

        fun isExpect(element: PsiModifierListOwner) =
            ownerExpect || element.isExpect

        val tooltip = MultiplatformInsightBundle.message("multiplatform.actual.implements-expect", "<b>${element.name}</b>")

        val implementations = expectClass.fields
            .filter { field ->
                field.name == element.name && isExpect(field)
            }
            .takeUnless(List<*>::isEmpty)
            ?: return null

        return LineMarkerInfo(
            element.nameIdentifier,
            element.nameIdentifier.textRange,
            AllIcons.Gutter.ImplementingMethod,
            { tooltip },
            DefaultGutterIconNavigationHandler(
                implementations,
                tooltip,
            ),
            GutterIconRenderer.Alignment.LEFT,
            { tooltip }
        )
    }

    private fun handle(element: PsiClass): LineMarkerInfo<*>? {
        if (!element.isActual) {
            return null
        }

        val tooltip = MultiplatformInsightBundle.message("multiplatform.actual.implements-expect", "<b>${element.name}</b>")

        val expectClass = element.expect ?: return null

        return LineMarkerInfo(
            element.nameIdentifier!!,
            element.nameIdentifier!!.textRange,
            AllIcons.Gutter.ImplementingMethod,
            { tooltip },
            DefaultGutterIconNavigationHandler(
                listOf(expectClass),
                tooltip,
            ),
            GutterIconRenderer.Alignment.LEFT,
            { tooltip }
        )
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return when (element) {
            is PsiMethod -> handle(element)
            is PsiField -> handle(element)
            is PsiClass -> handle(element)
            else -> null
        }
    }
}

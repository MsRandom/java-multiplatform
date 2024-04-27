package net.msrandom.multiplatform.lines

import com.intellij.codeInsight.daemon.DefaultGutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.icons.AllIcons
import com.intellij.lang.jvm.JvmParameter
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.*
import net.msrandom.multiplatform.*

class ExpectLineMarkerProviderDescriptor : LineMarkerProviderDescriptor() {
    override fun getName() =
        MultiplatformInsightBundle.message("multiplatform.expect.find-actual")

    private fun handle(element: PsiMethod): LineMarkerInfo<*>? {
        val owner = element.containingClass!!

        if (!element.isExpect && !owner.isExpect) {
            return null
        }

        val tooltip = MultiplatformInsightBundle.message("multiplatform.expect.actual-implementations", "<b>${element.name}</b>")

        val actualClasses = owner.actuals


        val implementations = actualClasses
            .map {
                element.actual(it)
            }
            .takeUnless(List<*>::isEmpty)
            ?: return null

        return LineMarkerInfo(
            element.nameIdentifier!!,
            element.nameIdentifier!!.textRange,
            AllIcons.Gutter.ImplementedMethod,
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
        val owner = element.containingClass!!

        if (!element.isExpect && !owner.isExpect) {
            return null
        }

        val tooltip = MultiplatformInsightBundle.message("multiplatform.expect.actual-implementations", "<b>${element.name}</b>")

        val actualClasses = owner.actuals

        val implementations = actualClasses
            .map { element.actual(it) }
            .takeUnless(List<*>::isEmpty)
            ?: return null

        return LineMarkerInfo(
            element.nameIdentifier,
            element.nameIdentifier.textRange,
            AllIcons.Gutter.ImplementedMethod,
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
        if (!element.isExpect) {
            return null
        }

        val tooltip = MultiplatformInsightBundle.message("multiplatform.expect.actual-implementations", "<b>${element.name}</b>")

        val actualClasses = element.actuals
            .filter(PsiClass::isActual)
            .takeUnless(List<*>::isEmpty)
            ?: return null

        return LineMarkerInfo(
            element.nameIdentifier!!,
            element.nameIdentifier!!.textRange,
            AllIcons.Gutter.ImplementedMethod,
            { tooltip },
            DefaultGutterIconNavigationHandler(
                actualClasses,
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

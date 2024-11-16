package net.msrandom.multiplatform.insight

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.quickfix.AddMethodBodyFix
import com.intellij.codeInsight.daemon.impl.quickfix.AddVariableInitializerFix
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.modcommand.PsiBasedModCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import net.msrandom.multiplatform.isExpect

private val myPointer = PsiBasedModCommandAction::class.java.getDeclaredField("myPointer").apply { isAccessible = true }

class ExpectHighlightErrorFilter : HighlightInfoFilter {
    override fun accept(info: HighlightInfo, file: PsiFile?): Boolean {
        if (info.severity != HighlightSeverity.ERROR && info.severity != HighlightSeverity.WARNING) {
            return true
        }

        val element = if (info.type.attributesKey == HighlightInfoType.UNUSED_SYMBOL.attributesKey) {
            val element = PsiTreeUtil.findElementOfClassAtOffset(file!!, info.startOffset, PsiParameterList::class.java, false)

            element?.parent
        } else {
            info.findRegisteredQuickFix { descriptor, _ ->
                val action = descriptor.action.asModCommandAction()

                if (action is AddMethodBodyFix || action is AddVariableInitializerFix) {
                    (myPointer[action] as SmartPsiElementPointer<*>).element
                } else {
                    null
                }
            }
        }

        if (element == null || element !is PsiJvmMember) {
            return true
        }

        val owner = element.containingClass!!

        return !element.isExpect && !owner.isExpect
    }
}

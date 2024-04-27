package net.msrandom.multiplatform.insight

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.quickfix.AddMethodBodyFix
import com.intellij.codeInsight.daemon.impl.quickfix.AddVariableInitializerFix
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import net.msrandom.multiplatform.isActual
import net.msrandom.multiplatform.isExpect

class ActualHighlightErrorFilter : HighlightInfoFilter {
    override fun accept(info: HighlightInfo, file: PsiFile?): Boolean {
        if (info.severity != HighlightSeverity.WARNING || info.type.attributesKey != HighlightInfoType.UNUSED_SYMBOL.attributesKey) {
            return true
        }

        val element = PsiTreeUtil.findElementOfClassAtOffset(file!!, info.startOffset, PsiModifierListOwner::class.java, false) ?: return true

        return !element.isActual
    }
}

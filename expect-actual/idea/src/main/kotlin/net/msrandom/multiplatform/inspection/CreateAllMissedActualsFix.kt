package net.msrandom.multiplatform.inspection

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiNamedElement
import net.msrandom.multiplatform.MultiplatformInsightBundle
import net.msrandom.multiplatform.implementingModules
import org.jetbrains.jps.model.java.JavaSourceRootType

class CreateAllMissedActualsFix<T>(
    declaration: T,
    private val notActualizedLeafModules: Collection<Module>,
) : LocalQuickFixAndIntentionActionOnPsiElement(declaration) where T : PsiModifierListOwner, T : PsiNamedElement {
    override fun getFamilyName() =
        MultiplatformInsightBundle.message("multiplatform.expect-actual.group")

    override fun getText() =
        MultiplatformInsightBundle.message("multiplatform.expect.create-all-missed-actuals")

    override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
        val moduleWithExpect = ModuleUtil.findModuleForPsiElement(startElement) ?: return
        val simpleModuleNames = moduleWithExpect.implementingModules.plus(moduleWithExpect).getSimpleSourceSetNames()

        val modulePaths = moduleWithExpect.implementingModules.associateWithTo(hashMapOf()) {
            val virtualFile = ModuleRootManager.getInstance(it).contentEntries.firstNotNullOf {
                it.sourceFolders.firstOrNull { it.rootType is JavaSourceRootType }
            }.file!!

            file.manager.findDirectory(virtualFile)!!
        }

        generateActualsForSelectedModules(
            project,
            editor,
            startElement as PsiModifierListOwner,
            notActualizedLeafModules,
            modulePaths,
            simpleModuleNames
        )
    }
}
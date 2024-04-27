package net.msrandom.multiplatform.inspection

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import net.msrandom.multiplatform.*

class NoMatchingActualInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun checkClass(aClass: PsiClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!aClass.isExpect) {
            return null
        }

        val module = ModuleUtil.findModuleForPsiElement(aClass)!!

        val neededModules = module.implementingModules

        val classActuals = aClass
            .actuals
            .filter(PsiClass::isActual)

        val implementedModules = classActuals.mapNotNull {
            ModuleUtil.findModuleForPsiElement(it)
        }

        val mismatchedClasses = classActuals.filter {
            it.typeParameters.size != aClass.typeParameters.size ||
                    it.typeParameters.zip(aClass.typeParameters)
                        .map { (a, b) -> a.bounds contentEquals b.bounds }
                        .any { !it }
        }

        val mismatchedImplementations = mismatchedClasses.mapNotNull {
            ModuleUtil.findModuleForPsiElement(it)
        }

        val startOffset = aClass.textRange.startOffset
        val afterAnnotations = aClass.annotations.lastOrNull()?.textRange?.endOffset ?: startOffset
        val bodyStart = aClass.lBrace!!.textRange.startOffset

        val range = TextRange.create(afterAnnotations - startOffset, bodyStart - startOffset)

        if (mismatchedImplementations.isNotEmpty()) {
            return arrayOf(
                manager.createProblemDescriptor(
                    aClass,
                    range,
                    MultiplatformInsightBundle.message(
                        "multiplatform.expect.mismatched-class",
                        aClass.name,
                        mismatchedImplementations.joinToString(transform = Module::getName)
                    ),
                    ProblemHighlightType.WARNING,
                    isOnTheFly,
                )
            )
        }

        val missingImplementations = neededModules - implementedModules.toSet()

        if (missingImplementations.isEmpty()) {
            return null
        }

        return arrayOf(
            manager.createProblemDescriptor(
                aClass,
                range,
                MultiplatformInsightBundle.message(
                    "multiplatform.expect.no-actual-class",
                    aClass.name,
                    missingImplementations.joinToString(transform = Module::getName)
                ),
                ProblemHighlightType.GENERIC_ERROR,
                isOnTheFly,
                CreateMissedActualsFix(aClass, missingImplementations),
                CreateAllMissedActualsFix(aClass, missingImplementations),
            )
        )
    }

    override fun checkMethod(method: PsiMethod, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val owner = method.containingClass!!

        if (!method.isExpect && !owner.isExpect) {
            return null
        }

        val module = ModuleUtil.findModuleForPsiElement(method)!!

        val neededModules = module.implementingModules

        val implementedModules = owner
            .actuals
            .filter(PsiClass::isActual)
            .mapNotNull { method.actual(it) }
            .mapNotNull { ModuleUtil.findModuleForPsiElement(it) }

        val missingImplementations = neededModules - implementedModules.toSet()

        if (missingImplementations.isEmpty()) {
            return null
        }

        return arrayOf(
            manager.createProblemDescriptor(
                method,
                method,
                MultiplatformInsightBundle.message(
                    "multiplatform.expect.no-actual-method",
                    method.name,
                    missingImplementations.joinToString(transform = Module::getName)
                ),
                ProblemHighlightType.GENERIC_ERROR,
                isOnTheFly,
                CreateMissedActualsFix(method, missingImplementations),
                CreateAllMissedActualsFix(method, missingImplementations),
            )
        )
    }

    override fun checkField(field: PsiField, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val owner = field.containingClass!!

        if (!field.isExpect && !owner.isExpect) {
            return null
        }

        val module = ModuleUtil.findModuleForPsiElement(field)!!

        val neededModules = module.implementingModules

        val implementedModules = owner
            .actuals
            .filter(PsiClass::isActual)
            .mapNotNull { field.actual(it) }
            .mapNotNull { ModuleUtil.findModuleForPsiElement(it) }

        val missingImplementations = neededModules - implementedModules.toSet()

        if (missingImplementations.isEmpty()) {
            return null
        }

        return arrayOf(
            manager.createProblemDescriptor(
                field,
                field,
                MultiplatformInsightBundle.message(
                    "multiplatform.expect.no-actual-field",
                    field.name,
                    missingImplementations.joinToString(transform = Module::getName)
                ),
                ProblemHighlightType.GENERIC_ERROR,
                isOnTheFly,
                CreateMissedActualsFix(field, missingImplementations),
                CreateAllMissedActualsFix(field, missingImplementations),
            )
        )
    }
}

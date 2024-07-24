package net.msrandom.multiplatform.inspection

import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.icons.AllIcons
import com.intellij.ide.wizard.setMinimumWidthForAllRowLabels
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.ComponentPredicate
import net.msrandom.multiplatform.ACTUAL_SUFFIX
import net.msrandom.multiplatform.MultiplatformInsightBundle
import net.msrandom.multiplatform.annotations.Actual
import net.msrandom.multiplatform.implementingModules
import org.jetbrains.jps.model.java.JavaSourceRootType
import java.io.File
import javax.swing.JComponent
import kotlin.io.path.Path

private fun PsiDirectory.getDirectory(subdirectory: String): PsiDirectory {
    val directoryNames = Path(subdirectory)

    var directory = this

    for (subPath in directoryNames) {
        val name = subPath.toString()
        directory = directory.findSubdirectory(name)
            ?: directory.createSubdirectory(name)
    }

    return directory
}

class CreateMissedActualsFix<T>(
    declaration: T,
    private val notActualizedLeafModules: Collection<Module>,
) : LocalQuickFixAndIntentionActionOnPsiElement(declaration) where T : PsiModifierListOwner, T : PsiNamedElement {
    override fun getFamilyName() =
        MultiplatformInsightBundle.message("multiplatform.expect-actual.group")

    override fun getText() =
        MultiplatformInsightBundle.message("multiplatform.expect.create-missed-actuals")

    override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
        val moduleWithExpect = ModuleUtil.findModuleForPsiElement(startElement) ?: return
        val simpleModuleNames = moduleWithExpect.implementingModules.plus(moduleWithExpect).getSimpleSourceSetNames()

        val application = ApplicationManager.getApplication()
        val testMode = application.isUnitTestMode || application.isHeadlessEnvironment

        val modulePaths = moduleWithExpect.implementingModules.associateWithTo(hashMapOf()) {
            val virtualFile = ModuleRootManager.getInstance(it).contentEntries.firstNotNullOf {
                it.sourceFolders.firstOrNull { it.rootType is JavaSourceRootType }
            }.file!!

            file.manager.findDirectory(virtualFile)!!
        }

        if (!testMode) {
            CreateMissedActualsDialog(
                project,
                editor,
                startElement as PsiModifierListOwner,
                moduleWithExpect,
                notActualizedLeafModules,
                modulePaths,
                simpleModuleNames
            ).show()
        } else {
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
}

internal fun Collection<Module>.getSimpleSourceSetNames(): Map<Module, String> {
    if (size < 2) return associateWith { it.name }

    val commonPrefix = map(Module::getName)
        .zipWithNext()
        .map { (a, b) ->
            a.commonPrefixWith(b)
        }
        .minBy(String::length)

    val prefixToRemove = commonPrefix
        .substringBeforeLast(".", "")
        .let {
            if (it.isEmpty()) {
                it
            } else {
                "$it."
            }
        }

    return associateWith {
        it.name.removePrefix(prefixToRemove)
    }
}

private class CreateMissedActualsDialog(
    private val project: Project,
    private val editor: Editor?,
    private val declaration: PsiModifierListOwner,
    private val moduleWithExpect: Module,
    private val notActualizedLeafModules: Collection<Module>,
    private val modulePaths: MutableMap<Module, PsiDirectory>,
    private val simpleModuleNames: Map<Module, String>
) : DialogWrapper(project, true) {
    private val selectedModules = mutableListOf<Module>()

    private val selectedModulesListeners = mutableListOf<() -> Unit>()

    init {
        title = MultiplatformInsightBundle.message("multiplatform.expect.create-missed-actuals")
        init()
    }

    override fun doOKAction() {
        super.doOKAction()

        generateActualsForSelectedModules(
            project,
            editor,
            declaration,
            selectedModules,
            modulePaths,
            simpleModuleNames
        )
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            panel {
                row {
                    label(MultiplatformInsightBundle.message("multiplatform.expect.choose-actual-modules"))
                }

                separator()

                moduleWithExpect.implementingModules.forEach { item ->
                    val row = row {
                        val checkbox = checkBox("").onChanged { cb ->
                            if (cb.isSelected) {
                                selectedModules.add(item)
                            } else {
                                selectedModules.remove(item)
                            }

                            selectedModulesListeners.forEach { listener -> listener() }
                        }

                        icon(AllIcons.Actions.ModuleDirectory)

                        label(simpleModuleNames.getOrDefault(item, item.name))

                        val psiManager = PsiManager.getInstance(project)
                        val projectPath = project.guessProjectDir()!!
                        val projectDirectory = psiManager.findDirectory(projectPath)!!

                        textField()
                            .label("")
                            .bindText(
                                {
                                    projectPath.toNioPath().relativize(modulePaths.getValue(item).virtualFile.toNioPath()).toString()
                                },
                                {
                                    modulePaths[item] = projectDirectory.getDirectory(it)
                                }
                            )
                            .enabled(true)
                            .visibleIf(checkbox.selected)
                    }

                    row.enabledIf(object : ComponentPredicate() {
                        override fun addListener(listener: (Boolean) -> Unit) {}

                        override fun invoke() = item in notActualizedLeafModules
                    })
                }
            }
        }
    }.apply {
        withMinimumWidth(500)
        setMinimumWidthForAllRowLabels(90)
    }
}

internal fun generateActualsForSelectedModules(
    project: Project,
    editor: Editor?,
    declaration: PsiModifierListOwner,
    selectedModules: Collection<Module>,
    modulePaths: Map<Module, PsiDirectory>,
    simpleModuleNames: Map<Module, String>
) {
    val sourceFolderManager = SourceFolderManager.getInstance(project)

    sourceFolderManager.rescanAndUpdateSourceFolders()

    executeCommand(project, MultiplatformInsightBundle.message("multiplatform.expect.create-missed-actuals")) {
        runWriteAction {
            val fileEditorManager = FileEditorManager.getInstance(project)
            val elementFactory = PsiElementFactory.getInstance(project)
            val psiFacade = JavaPsiFacade.getInstance(project)

            selectedModules.forEach { module ->
                val directory = modulePaths.getValue(module).getDirectory((declaration.containingFile as PsiJavaFile).packageName.replace('.', File.separatorChar))

                val makeActual = { psiClass: PsiClass ->
                    psiFacade.findClass(psiClass.qualifiedName + ACTUAL_SUFFIX, module.moduleScope) ?: run {
                        val kind = CreateClassKind.valueOf(psiClass.classKind.name)

                        kind.createInDirectory(directory, psiClass.name + ACTUAL_SUFFIX)
                    }
                }

                val actualAnnotation = psiFacade
                    .findClass(Actual::class.qualifiedName!!, GlobalSearchScope.moduleWithLibrariesScope(module))!!

                val actualImport = elementFactory.createImportStatement(actualAnnotation)

                val actualClass = when (declaration) {
                    is PsiClass -> create(declaration, elementFactory, actualImport, makeActual)
                    is PsiMethod -> create(declaration, elementFactory, makeActual)
                    is PsiField -> create(declaration, elementFactory, makeActual)
                    else -> throw UnsupportedOperationException("Unsupported declaration for @Expect/@Actual $declaration")
                }

                fileEditorManager.openFile(actualClass.containingFile.virtualFile)
            }
        }
    }
}

package net.msrandom.multiplatform

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.modules
import net.msrandom.multiplatform.gradle.VIRTUAL_SOURCE_SETS_MODEL
import net.msrandom.multiplatform.gradle.VirtualSourceSet
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.util.GradleConstants

private class SourceSetWithDependants(val name: String, val dependants: MutableSet<SourceSetWithDependants>)

val Module.implementingModules: List<Module>
    get() {
        val projectPath = ExternalSystemApiUtil.getExternalRootProjectPath(this) ?: return emptyList()
        val moduleData = ExternalSystemApiUtil.findModuleNode(project, GradleConstants.SYSTEM_ID, projectPath) ?: return emptyList()

        val virtualSourceSetModel = ExternalSystemApiUtil.find(moduleData, VIRTUAL_SOURCE_SETS_MODEL) ?: return emptyList()
        val sourceSetData = ExternalSystemApiUtil.findChild(moduleData, GradleSourceSetData.KEY) { it.data.internalName == name } ?: return emptyList()

        val modules = hashMapOf<String, SourceSetWithDependants>()

        fun handleSourceSet(virtual: VirtualSourceSet): SourceSetWithDependants {
            modules[virtual.name]?.let { return it }

            val sourceSet = SourceSetWithDependants(name, hashSetOf())

            modules[virtual.name] = sourceSet

            for (dependency in virtual.dependencies) {
                handleSourceSet(dependency).dependants.add(sourceSet)
            }

            return sourceSet
        }

        for (sourceSet in virtualSourceSetModel.data.sourceSets) {
            handleSourceSet(sourceSet)
        }

        fun getAllDependants(sourceSet: SourceSetWithDependants): Set<SourceSetWithDependants> =
            sourceSet.dependants.flatMapTo(hashSetOf(), ::getAllDependants)

        return getAllDependants(modules.getValue(sourceSetData.data.moduleName)).map { sourceSet ->
            project.modules.first { module ->
                sourceSet.name == module.name
            }
        }
    }

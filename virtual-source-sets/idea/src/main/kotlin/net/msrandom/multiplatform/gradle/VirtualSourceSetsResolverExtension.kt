package net.msrandom.multiplatform.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

val VIRTUAL_SOURCE_SETS_MODEL = Key.create(VirtualSourceSetModel::class.java, 0)

class VirtualSourceSetsResolverExtension : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses() =
        setOf(VirtualSourceSetModel::class.java)

    override fun getToolingExtensionsClasses() =
        setOf(VirtualSourceSetModelBuilder::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        super.populateModuleExtraModels(gradleModule, ideModule)

        val model = resolverCtx.getExtraProject(gradleModule, VirtualSourceSetModel::class.java) ?: return

        ideModule.createChild(VIRTUAL_SOURCE_SETS_MODEL, model)
    }
}

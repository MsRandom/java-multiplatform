package net.msrandom.multiplatform.gradle

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext

@CompileStatic
class VirtualSourceSetModelBuilder extends AbstractModelBuilderService {
    private static final String VIRTUAL_SOURCE_SET_MODEL_NAME = "net.msrandom.virtualsourcesets.model.VirtualSourceSetsModel"

    @CompileDynamic
    private static VirtualSourceSet mapSourceSet(sourceSet) {
        String name = sourceSet.name
        List dependencies = sourceSet.dependencies

        new VirtualSourceSet(name, dependencies.collect { mapSourceSet(it) })
    }

    @CompileDynamic
    private static List getSourceSets(model) {
        return model.sourceSets
    }

    boolean canBuild(String modelName) {
        modelName == VirtualSourceSetModel.name
    }

    VirtualSourceSetModel buildAll(String modelName, Project project, ModelBuilderContext modelBuilderContext) {
        if (!project.plugins.hasPlugin("java-virtual-source-sets")) {
            return null
        }

        def model = (project as ProjectInternal)
            .services
            .get(ToolingModelBuilderRegistry)
            .getBuilder(VIRTUAL_SOURCE_SET_MODEL_NAME)
            .buildAll(VIRTUAL_SOURCE_SET_MODEL_NAME, project)

        new VirtualSourceSetModelImpl(getSourceSets(model).collect {
            mapSourceSet(it)
        })
    }
}

package net.msrandom.virtualsourcesets

import net.msrandom.virtualsourcesets.model.VirtualSourceSetModelBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import javax.inject.Inject

@Suppress("unused")
open class JavaVirtualSourceSetsPlugin @Inject constructor(private val modelBuilderRegistry: ToolingModelBuilderRegistry) : Plugin<Project>  {
    private fun Project.extend(base: String, dependency: String) = project.configurations.findByName(dependency)?.let {
        project.configurations.findByName(base)?.extendsFrom(it)
    }

    private fun SourceSet.addDependency(dependency: SourceSet, project: Project) {
        project.extend(apiConfigurationName, dependency.apiConfigurationName)
        project.extend(compileOnlyApiConfigurationName, dependency.compileOnlyApiConfigurationName)
        project.extend(implementationConfigurationName, dependency.implementationConfigurationName)
        project.extend(runtimeOnlyConfigurationName, dependency.runtimeOnlyConfigurationName)
        project.extend(compileOnlyConfigurationName, dependency.compileOnlyConfigurationName)

        if (System.getProperty("idea.sync.active", "false").toBoolean()) {
            compileClasspath += dependency.output
        }

        java.source(dependency.java)
        resources.source(dependency.resources)
    }

    override fun apply(target: Project) {
        target.plugins.apply(JavaPlugin::class.java)

        target.extensions.getByType(SourceSetContainer::class.java).all { sourceSet ->
            val virtual = sourceSet.extensions.create("virtual", VirtualExtension::class.java, target.objects)

            virtual.dependsOn.all { dependency ->
                sourceSet.addDependency(dependency, target)
            }
        }

        modelBuilderRegistry.register(VirtualSourceSetModelBuilder())
    }
}

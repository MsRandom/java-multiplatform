package net.msrandom.virtualsourcesets

import net.msrandom.virtualsourcesets.model.VirtualSourceSetModelBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.HasMultipleValues
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformStructure
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import javax.inject.Inject
import kotlin.collections.any
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Suppress("UNCHECKED_CAST")
private val commonSourceSet = KotlinCompile::class.memberProperties
    .first { it.name == "commonSourceSet" }
    .apply { isAccessible = true } as KProperty1<KotlinCompile, ConfigurableFileCollection>

private const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"

@Suppress("unused")
open class JavaVirtualSourceSetsPlugin @Inject constructor(private val modelBuilderRegistry: ToolingModelBuilderRegistry) : Plugin<Project> {
    private fun Project.extend(base: String, dependency: String) = project.configurations.findByName(dependency)?.let {
        project.configurations.findByName(base)?.extendsFrom(it)
    }

    private fun SourceSet.addJavaSources(dependency: SourceSet) {
        java.source(dependency.java)
        resources.source(dependency.resources)

        dependency.extensions.getByType(VirtualExtension::class.java).dependsOn.all {
            addJavaSources(it)
        }
    }

    @OptIn(InternalKotlinGradlePluginApi::class)
    private fun SourceSet.addKotlinCommonSources(kotlin: KotlinJvmProjectExtension, dependency: SourceSet, compileTask: KotlinCompile) {
        val kotlinSourceSet = kotlin.sourceSets.getByName(name)
        val kotlinDependency = kotlin.sourceSets.getByName(dependency.name)

        val isK2 = compileTask.compilerOptions.languageVersion
            .orElse(KotlinVersion.DEFAULT)
            .map { it >= KotlinVersion.KOTLIN_2_0 }

        fun <T> addK2Argument(property: HasMultipleValues<T>, value: () -> T) {
            property.addAll(isK2.map {
                if (it) {
                    listOf(value())
                } else {
                    emptyList()
                }
            })
        }

        fun addFragment(sourceSet: KotlinSourceSet) {
            if (compileTask.multiplatformStructure.fragments.get().any { it.fragmentName == sourceSet.name }) {
                return
            }

            addK2Argument(compileTask.multiplatformStructure.fragments) {
                K2MultiplatformStructure.Fragment(sourceSet.name, sourceSet.kotlin.asFileTree)
            }
        }

        addK2Argument(compileTask.multiplatformStructure.refinesEdges) {
            K2MultiplatformStructure.RefinesEdge(kotlinSourceSet.name, kotlinDependency.name)
        }

        addFragment(kotlinSourceSet)
        addFragment(kotlinDependency)

        commonSourceSet.get(compileTask).from(kotlinDependency.kotlin)
        compileTask.source(kotlinDependency.kotlin)

        dependency.extensions.getByType(VirtualExtension::class.java).dependsOn.all {
            dependency.addKotlinCommonSources(kotlin, it, compileTask)
        }
    }

    private fun SourceSet.addDependency(dependency: SourceSet, project: Project) {
        project.extend(apiConfigurationName, dependency.apiConfigurationName)
        project.extend(compileOnlyApiConfigurationName, dependency.compileOnlyApiConfigurationName)
        project.extend(implementationConfigurationName, dependency.implementationConfigurationName)
        project.extend(runtimeOnlyConfigurationName, dependency.runtimeOnlyConfigurationName)
        project.extend(compileOnlyConfigurationName, dependency.compileOnlyConfigurationName)

        if (System.getProperty("idea.sync.active")?.toBoolean() == true) {
            // TODO Temporary until an intellij plugin is complete
            compileClasspath += dependency.output
        }

        addJavaSources(dependency)

        project.plugins.withId(KOTLIN_JVM) {
            val kotlin = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
            val kotlinCompilation = kotlin.target.compilations.getByName(name)

            project.tasks.named(kotlinCompilation.compileKotlinTaskName, KotlinCompile::class.java) {
                it.multiPlatformEnabled.set(true)

                addKotlinCommonSources(kotlin, dependency, it)
            }
        }
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

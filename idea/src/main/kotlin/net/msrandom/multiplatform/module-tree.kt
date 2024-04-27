package net.msrandom.multiplatform

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.intellij.facet.FacetManager
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.SourceFolder

private val moduleTreeCache = hashMapOf<Project, Graph<Module>>()

val Module.isTestModule
    get() = name.endsWith("test", ignoreCase = true) ||
            ModuleRootManager.getInstance(this)
                .contentEntries
                .any {
                    it.sourceFolders.any(SourceFolder::isTestSource)
                }

fun Module.isAndroidModule(modelsProvider: IdeModifiableModelsProvider? = null): Boolean {
    val facetModel = modelsProvider?.getModifiableFacetModel(this) ?: FacetManager.getInstance(this)
    val facets = facetModel.allFacets
    return facets.any { it.javaClass.simpleName == "AndroidFacet" }
}

fun Module.isChild(other: Module) =
    ModuleRootManager.getInstance(other).dependencies.contains(this)

private val Project.moduleTree: Graph<Module>
    get() = moduleTreeCache.computeIfAbsent(this) { project ->
        val graph = GraphBuilder.directed().build<Module>()

        for (module in project.modules) {
            if (module.isTestModule) {
                continue
            }

            for (otherModule in project.modules) {
                if (module == otherModule || otherModule.isTestModule) {
                    continue
                }

                graph.addNode(module)
                graph.addNode(otherModule)

                if (module.isChild(otherModule)) {
                    graph.putEdge(module, otherModule)
                } else if (otherModule.isChild(module)) {
                    graph.putEdge(otherModule, module)
                }
            }
        }

        graph
    }

val Module.implementingModules
    get() = project.moduleTree.incidentEdges(this).map(EndpointPair<Module>::target)

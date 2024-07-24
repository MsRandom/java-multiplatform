package net.msrandom.multiplatform.gradle

import java.io.Serializable

class VirtualSourceSet(val name: String, val dependencies: List<VirtualSourceSet>) : Serializable

sealed interface VirtualSourceSetModel {
    val sourceSets: List<VirtualSourceSet>
}

class VirtualSourceSetModelImpl(override val sourceSets: List<VirtualSourceSet>) : VirtualSourceSetModel, Serializable

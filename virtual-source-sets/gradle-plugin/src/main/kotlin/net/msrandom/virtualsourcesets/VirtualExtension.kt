package net.msrandom.virtualsourcesets

import org.gradle.api.DomainObjectCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import javax.inject.Inject

open class VirtualExtension(objectFactory: ObjectFactory) {
    val dependsOn: DomainObjectCollection<SourceSet> = objectFactory.domainObjectSet(SourceSet::class.java)
}

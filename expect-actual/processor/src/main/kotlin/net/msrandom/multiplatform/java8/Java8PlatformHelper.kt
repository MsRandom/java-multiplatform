package net.msrandom.multiplatform.java8

import net.msrandom.multiplatform.bootstrap.ElementRemover
import net.msrandom.multiplatform.bootstrap.PlatformHelper

object Java8PlatformHelper : PlatformHelper {
    override val elementRemover: ElementRemover
        get() = Java8ElementRemover

    override fun addExports(processorClass: Class<*>) {}
}

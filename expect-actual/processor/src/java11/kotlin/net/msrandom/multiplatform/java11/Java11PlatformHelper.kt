package net.msrandom.multiplatform.java11

import net.msrandom.multiplatform.bootstrap.ElementRemover
import net.msrandom.multiplatform.bootstrap.PlatformHelper
import sun.misc.Unsafe

private val UNSAFE = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }[null] as Unsafe

@Suppress("unused")
private class MethodStub {
    private var override = false
}

class Java11PlatformHelper : PlatformHelper {
    override val elementRemover: ElementRemover
        get() = Java11ElementRemover

    override fun addExports(processorClass: Class<*>) {
        val jdkCompilerModule = ModuleLayer.boot().findModule("jdk.compiler").get()
        val ownModule: Any = processorClass.module

        val requiredPackages = arrayOf(
            "com.sun.tools.javac.api",
            "com.sun.tools.javac.code",
            "com.sun.tools.javac.comp",
            // "com.sun.tools.javac.file",
            "com.sun.tools.javac.main",
            "com.sun.tools.javac.model",
            // "com.sun.tools.javac.parser",
            "com.sun.tools.javac.processing",
            "com.sun.tools.javac.tree",
            "com.sun.tools.javac.util",
            //"com.sun.tools.javac.jvm",
        )

        val addExports = Module::class.java.getDeclaredMethod("implAddExports", String::class.java, Module::class.java)

        UNSAFE.putBooleanVolatile(addExports, UNSAFE.objectFieldOffset(MethodStub::class.java.getDeclaredField("override")), true)

        for (pkg in requiredPackages) {
            addExports.invoke(jdkCompilerModule, pkg, ownModule)
        }
    }
}

package net.msrandom.classextensions.kotlin.plugin

import kotlin.test.Test
import org.jetbrains.kotlin.cli.jvm.main as kotlinc

class CodegenTest {
    @Test
    fun `Test Codegen`() {
        val classpath = System.getProperty("java.class.path")
        val annotations = "../annotations/build/libs/class-extension-annotations-1.0.0.jar"

        kotlinc(arrayOf("src/test/resources/test.kt", "-language-version", "2.0", "-no-stdlib", "-classpath", "$classpath:$annotations", "-Xcompiler-plugin=build/libs/class-extension-kotlin-plugin-1.0.8.jar"))
    }
}

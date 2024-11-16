package a.b.c

import net.msrandom.classextensions.ClassExtension
import net.msrandom.classextensions.ExtensionShadow
import net.msrandom.classextensions.ExtensionInject

class A {
    val a: Int = 1
}

internal interface I {}

@ClassExtension(A::class)
class AExtension : a.b.c.I {
    @ExtensionShadow
    val a: Int = 0

    @get:ExtensionInject
    val b: Int
        get() = a

    @ExtensionInject
    fun injected() {
        println(b)
    }
}

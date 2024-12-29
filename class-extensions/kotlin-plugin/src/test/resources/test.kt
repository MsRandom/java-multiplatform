package a.b.c

import net.msrandom.classextensions.ClassExtension
import net.msrandom.classextensions.ExtensionInject
import net.msrandom.classextensions.ExtensionShadow

class A {
    val a: Int = 1

    fun toBeShadowed() {
        println("Shadowed!")
    }
}

interface I {
    fun hi()
}

@ClassExtension(A::class)
class AExtension : I {
    @ExtensionShadow
    val a: Int = TODO()

    val b: Int
        @ExtensionInject get() = a

    @ExtensionInject
    private var c = 5

    @ExtensionShadow
    fun toBeShadowed(): Unit = TODO()

    @ExtensionInject
    override fun hi() {
        println("hi")
    }

    @ExtensionInject
    fun injected() {
        println(b)
    }
}

fun main() {
    println(A().hi())
    println(A().b)
}

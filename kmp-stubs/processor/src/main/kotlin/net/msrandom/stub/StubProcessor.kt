package net.msrandom.stub

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunction
import com.google.devtools.ksp.symbol.KSType

class StubProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val toStub = resolver.getSymbolsWithAnnotation("net.msrandom.stub.Stub")

        for (annotated in toStub) {
            if (annotated is KSType) {

            } else if (annotated is KSFunction) {

            }
        }

        environment.codeGenerator.createNewFile("")

        return emptyList()
    }
}

@AutoService(SymbolProcessorProvider::class)
class StubProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) =
        StubProcessor(environment)
}

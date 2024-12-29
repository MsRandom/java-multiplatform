package net.msrandom.classextensions.kotlin.plugin.copiers

import net.msrandom.classextensions.kotlin.plugin.ClassExtensionFirDeclarations.Key
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructorCopy
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol

fun copyConstructor(original: FirConstructor, ownerSymbol: FirClassSymbol<*>): FirConstructor {
    val constructorCopy = buildConstructorCopy(original) {
        symbol = FirConstructorSymbol(ownerSymbol.classId)
        origin = Key.origin
        this.moduleData = ownerSymbol.moduleData
    }

    return constructorCopy.transformChildren(FirFunctionCopier(original.symbol, ownerSymbol), Unit) as FirConstructor
}

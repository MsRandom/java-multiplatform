package net.msrandom.classextensions.kotlin.plugin

import net.msrandom.classextensions.kotlin.plugin.copiers.copyConstructor
import net.msrandom.classextensions.kotlin.plugin.copiers.copyProperty
import net.msrandom.classextensions.kotlin.plugin.copiers.copySimpleFunction
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

@OptIn(SymbolInternals::class)
object SymbolCopyProvider {
    private val functionSymbols = hashMapOf<Pair<CallableId, List<FirValueParameterSymbol>>, FirNamedFunctionSymbol>()
    private val propertySymbols = hashMapOf<CallableId, FirPropertySymbol>()
    private val constructorSymbols = hashMapOf<ClassId, FirConstructorSymbol>()

    fun copyIfNeeded(symbol: FirNamedFunctionSymbol, ownerSymbol: FirClassSymbol<*>): FirNamedFunctionSymbol {
        val existing = ownerSymbol.declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>().firstOrNull {
            it.name == symbol.callableId.callableName && it.valueParameterSymbols == symbol.valueParameterSymbols
        }

        if (existing != null) {
            return existing
        }

        val callableId = CallableId(ownerSymbol.classId, symbol.name)

        return functionSymbols.computeIfAbsent(callableId to symbol.valueParameterSymbols) { (callableId, _) ->
            copySimpleFunction(symbol.fir, ownerSymbol, callableId).symbol
        }
    }

    fun copyIfNeeded(symbol: FirPropertySymbol, ownerSymbol: FirClassSymbol<*>): FirPropertySymbol {
        val existing = ownerSymbol.declarationSymbols.filterIsInstance<FirPropertySymbol>().firstOrNull {
            it.name == symbol.callableId.callableName
        }

        if (existing != null) {
            return existing
        }

        val callableId = CallableId(ownerSymbol.classId, symbol.name)

        return propertySymbols.computeIfAbsent(callableId) {
            copyProperty(symbol.fir, ownerSymbol, it).symbol
        }
    }

    fun copyIfNeeded(symbol: FirConstructorSymbol, ownerSymbol: FirClassSymbol<*>): FirConstructorSymbol {
        val existing = ownerSymbol.declarationSymbols.filterIsInstance<FirConstructorSymbol>().firstOrNull {
            it.name == symbol.callableId.callableName && it.valueParameterSymbols == symbol.valueParameterSymbols
        }

        if (existing != null) {
            return existing
        }

        return constructorSymbols.computeIfAbsent(ownerSymbol.classId) {
            copyConstructor(symbol.fir, ownerSymbol).symbol
        }
    }
}

package net.msrandom.classextensions.kotlin.plugin.copiers

import net.msrandom.classextensions.kotlin.plugin.ClassExtensionFirDeclarations.Key
import net.msrandom.classextensions.kotlin.plugin.SymbolCopyProvider
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.builder.buildThisReceiverExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.CallableId

open class FirFunctionCopier(
    private val callableSymbol: FirCallableSymbol<*>,
    private val ownerSymbol: FirClassSymbol<*>,
) : FirTransformer<Unit>() {
    internal fun ConeKotlinType.replaceType() =
        replaceType(callableSymbol.callableId.classId!!, ownerSymbol.classId)

    override fun transformThisReference(thisReference: FirThisReference, data: Unit): FirReference {
        return buildImplicitThisReference {
            boundSymbol = ownerSymbol
            contextReceiverNumber = thisReference.contextReceiverNumber
            diagnostic = thisReference.diagnostic
        }
    }

    override fun transformResolvedNamedReference(
        resolvedNamedReference: FirResolvedNamedReference,
        data: Unit,
    ): FirReference {
        val symbol = resolvedNamedReference.resolvedSymbol

        if (symbol is FirCallableSymbol && symbol.callableId.classId == callableSymbol.callableId.classId) {
            // Needs second pass transformation, as it can refer to other transformed elements
            return super.transformNamedReference(buildResolvedNamedReference {
                source = resolvedNamedReference.source
                name = resolvedNamedReference.name

                resolvedSymbol = when (val symbol = resolvedNamedReference.resolvedSymbol) {
                    is FirNamedFunctionSymbol -> SymbolCopyProvider.copyIfNeeded(symbol, ownerSymbol)
                    is FirPropertySymbol -> SymbolCopyProvider.copyIfNeeded(symbol, ownerSymbol)
                    is FirConstructorSymbol -> SymbolCopyProvider.copyIfNeeded(symbol, ownerSymbol)
                    else -> resolvedNamedReference.resolvedSymbol
                }
            }, data)
        }

        return super.transformResolvedNamedReference(resolvedNamedReference, data)
    }

    @OptIn(UnresolvedExpressionTypeAccess::class)
    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: Unit
    ): FirStatement {
        return super.transformThisReceiverExpression(buildThisReceiverExpression {
            coneTypeOrNull = thisReceiverExpression.coneTypeOrNull?.replaceType()
            annotations.addAll(thisReceiverExpression.annotations)
            contextReceiverArguments.addAll(thisReceiverExpression.contextReceiverArguments)
            typeArguments.addAll(thisReceiverExpression.typeArguments)
            source = thisReceiverExpression.source
            nonFatalDiagnostics.addAll(thisReceiverExpression.nonFatalDiagnostics)
            calleeReference = thisReceiverExpression.calleeReference
            isImplicit = thisReceiverExpression.isImplicit
        }, data)
    }

    override fun <E : FirElement> transformElement(element: E, data: Unit): E {
        return element.transformChildren(this, Unit) as E
    }
}

@OptIn(UnresolvedExpressionTypeAccess::class)
fun copySimpleFunction(
    original: FirSimpleFunction,
    ownerSymbol: FirClassSymbol<*>,
    callableId: CallableId,
): FirSimpleFunction {
    val transformer = FirFunctionCopier(original.symbol, ownerSymbol)

    val functionCopy = buildSimpleFunctionCopy(original) {
        symbol = FirNamedFunctionSymbol(callableId)
        origin = Key.origin
        this.moduleData = ownerSymbol.moduleData

        with(transformer) {
            dispatchReceiverType = original.dispatchReceiverType?.replaceType()
        }
    }

    return functionCopy.transformChildren(transformer, Unit) as FirSimpleFunction
}

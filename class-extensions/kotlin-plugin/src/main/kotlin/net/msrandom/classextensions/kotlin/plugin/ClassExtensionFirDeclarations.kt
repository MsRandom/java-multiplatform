package net.msrandom.classextensions.kotlin.plugin

import net.msrandom.classextensions.ExtensionInject
import net.msrandom.classextensions.kotlin.plugin.FirClassExtensionResolver.Companion.classId
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class ClassExtensionFirDeclarations(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val resolver = FirClassExtensionResolver(session)

    @OptIn(SymbolInternals::class)
    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        return getCallables(context!!.owner)
            .filterIsInstance<FirNamedFunctionSymbol>()
            .filter { it.name == callableId.callableName }
            .map {
                SymbolCopyProvider.copyIfNeeded(it, context.owner)
            }
    }

    @OptIn(SymbolInternals::class)
    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirPropertySymbol> {
        return getCallables(context!!.owner)
            .filterIsInstance<FirPropertySymbol>()
            .filter { it.name == callableId.callableName }
            .map {
                SymbolCopyProvider.copyIfNeeded(it, context.owner)
            }
    }

    @OptIn(SymbolInternals::class)
    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        return getCallables(context.owner)
            .filterIsInstance<FirConstructorSymbol>()
            .map {
                SymbolCopyProvider.copyIfNeeded(it, context.owner)
            }
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> =
        getCallables(classSymbol).map {
            val name = it.callableId.callableName

            if (name == classSymbol.name) {
                SpecialNames.INIT
            } else {
                name
            }
        }.toHashSet()

    override fun FirDeclarationPredicateRegistrar.registerPredicates() = register(FirClassExtensionResolver.PREDICATE)

    private fun isInject(extension: FirClassSymbol<*>, member: FirCallableSymbol<*>?) = member
        ?.annotations
        ?.any { annotation ->
            val type = annotation.annotationTypeRef

            val resolvedType = if (type is FirUserTypeRef) {
                resolver.resolveType(extension, type)
            } else {
                type
            }

            return resolvedType.coneType.classId == extensionInject
        } == true

    private fun getCallables(classSymbol: FirClassSymbol<*>) = resolver.getExtensions(classSymbol)
        .flatMap { extension ->
            extension.declarationSymbols.mapNotNull {
                if (it !is FirCallableSymbol) {
                    return@mapNotNull null
                }

                if (isInject(extension, it)) {
                    return@mapNotNull it
                }

                if (it !is FirPropertySymbol) {
                    return@mapNotNull null
                }

                if (isInject(extension, it.getterSymbol) || isInject(extension, it.setterSymbol) || isInject(extension, it.backingFieldSymbol) || isInject(extension, it.delegateFieldSymbol)) {
                    return@mapNotNull it
                }

                return@mapNotNull null
            }
        }
        .toList()

    private companion object {
        private val extensionInject = classId(ExtensionInject::class.java)
    }

    internal object Key : GeneratedDeclarationKey()
}

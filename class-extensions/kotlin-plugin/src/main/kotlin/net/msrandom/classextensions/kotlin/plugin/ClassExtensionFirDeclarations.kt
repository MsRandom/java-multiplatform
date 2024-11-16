package net.msrandom.classextensions.kotlin.plugin

import net.msrandom.classextensions.ClassExtension
import net.msrandom.classextensions.ExtensionInject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructorCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.*

class ClassExtensionFirDeclarations(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val resolver = FirClassExtensionResolver(session)

    @OptIn(SymbolInternals::class)
    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val functions = getCallables(context!!.owner).filterIsInstance<FirNamedFunctionSymbol>().filter { it.name == callableId.callableName }

        if (functions.isNotEmpty()) {
            return functions.map {
                buildSimpleFunctionCopy(it.fir) {
                    symbol = FirNamedFunctionSymbol(callableId)
                }.symbol
            }
        }

        return super.generateFunctions(callableId, context)
    }

    @OptIn(SymbolInternals::class)
    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val properties = getCallables(context!!.owner).filterIsInstance<FirPropertySymbol>().filter { it.name == callableId.callableName }

        if (properties.isNotEmpty()) {
            return properties.map {
                buildPropertyCopy(it.fir) {
                    symbol = FirPropertySymbol(callableId)
                }.symbol
            }
        }

        return super.generateProperties(callableId, context)
    }

    @OptIn(SymbolInternals::class)
    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val constructors = getCallables(context.owner).filterIsInstance<FirConstructorSymbol>()

        if (constructors.isNotEmpty()) {
            return constructors.map {
                buildConstructorCopy(it.fir) {
                    symbol = FirConstructorSymbol(CallableId(context.owner.packageFqName(), context.owner.classId.relativeClassName, SpecialNames.INIT))
                }.symbol
            }
        }

        return super.generateConstructors(context)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> = getCallables(classSymbol).map {
        val name = it.callableId.callableName

        if (name == classSymbol.name) {
            SpecialNames.INIT
        } else {
            name
        }
    }.toHashSet()

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(LookupPredicate.AnnotatedWith(setOf(AnnotationFqn(ClassExtension::class.java.name))))
    }

    private fun isInject(extension: FirClassSymbol<*>, member: FirCallableSymbol<*>?) = member?.annotations?.any { annotation ->
        val type = annotation.annotationTypeRef

        val resolvedType = if (type is FirUserTypeRef) {
            resolver.resolveType(extension, type)
        } else {
            type
        }

        return resolvedType.coneType.classId == extensionInject
    } == true

    private fun getCallables(classSymbol: FirClassSymbol<*>) = resolver.getExtensions(classSymbol).flatMap { extension ->
        extension.declarationSymbols.mapNotNull {
            if (it !is FirCallableSymbol) {
                return@mapNotNull null
            }

            if (it is FirPropertySymbol) {
                if (isInject(extension, it.getterSymbol) || isInject(extension, it.setterSymbol)) {
                    return@mapNotNull it
                }

                return@mapNotNull null
            }

            if (!isInject(extension, it)) {
                return@mapNotNull null
            }

            it
        }
    }.toList()

    private companion object {
        val extensionInject = classId(ExtensionInject::class.java)

        private fun classId(cls: Class<*>) = FqName(cls.name).let {
            ClassId(it.parent(), it.shortName())
        }
    }
}

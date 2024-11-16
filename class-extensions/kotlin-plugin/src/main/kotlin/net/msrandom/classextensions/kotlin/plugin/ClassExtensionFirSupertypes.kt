package net.msrandom.classextensions.kotlin.plugin

import net.msrandom.classextensions.ClassExtension
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef

class ClassExtensionFirSupertypes(session: FirSession) : FirSupertypeGenerationExtension(session) {
    private val resolver = FirClassExtensionResolver(session)

    override fun computeAdditionalSupertypes(classLikeDeclaration: FirClassLikeDeclaration, resolvedSupertypes: List<FirResolvedTypeRef>, typeResolver: TypeResolveService): List<FirResolvedTypeRef> {
        val symbol = classLikeDeclaration.symbol

        if (symbol !is FirClassSymbol<*>) {
            return emptyList()
        }

        val newSuperTypes: List<FirTypeRef> = resolver.getExtensions(symbol).flatMap { it.resolvedSuperTypeRefs }.toList()

        return newSuperTypes.map {
            if (it is FirUserTypeRef) {
                typeResolver.resolveUserType(it)
            } else {
                it as FirResolvedTypeRef
            }
        }
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        return declaration.symbol is FirClassSymbol<*> && resolver.getExtensions(declaration.symbol as FirClassSymbol<*>).any { it.resolvedSuperTypeRefs.isNotEmpty() }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(LookupPredicate.AnnotatedWith(setOf(AnnotationFqn(ClassExtension::class.java.name))))
    }
}

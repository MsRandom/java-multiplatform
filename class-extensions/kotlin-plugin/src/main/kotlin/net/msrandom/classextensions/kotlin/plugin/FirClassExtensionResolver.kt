package net.msrandom.classextensions.kotlin.plugin

import net.msrandom.classextensions.ClassExtension
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.ScopeClassDeclaration
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class FirClassExtensionResolver(private val session: FirSession) {
    private val classExtensions by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(PREDICATE)
    }

    fun getExtensions(expected: FirClassSymbol<*>) = classExtensions.asSequence().mapNotNull {
        if (it !is FirRegularClassSymbol) return@mapNotNull null

        val annotation = it.getAnnotationByClassId(classExtensionAnnotation, session)!!
        val value = annotation.findArgumentByName(Name.identifier(ClassExtension::value.name)) ?: return@mapNotNull null

        val file = session.firProvider.getFirClassifierContainerFileIfAny(expected) ?: return@mapNotNull null
        val typeName = (value as FirCall).argument.source?.lighterASTNode?.toString() ?: return@mapNotNull null

        val annotationTarget = resolveType(file, typeName) ?: return@mapNotNull null

        if (annotationTarget == expected) {
            it
        } else {
            null
        }
    }

    @OptIn(SymbolInternals::class)
    internal fun resolveType(currentClass: FirClassSymbol<*>, type: FirUserTypeRef): FirResolvedTypeRef {
        val file = session.firProvider.getFirClassifierContainerFile(currentClass)
        val scopes = createImportingScopes(file, session, ScopeSession())
        val firSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(session, expandTypeAliases = true)

        return firSpecificTypeResolverTransformer.transformTypeRef(type, ScopeClassDeclaration(scopes, listOf(currentClass.fir)))
    }

    // TODO Should be heavily tested, or replaced with functionality directly from the compiler to resolve the type name
    private fun resolveType(file: FirFile, typeName: String): FirClassLikeSymbol<*>? {
        val segments = typeName.split(".")

        var nameEnd = segments.size

        var pkg: FqName? = null

        while (pkg == null && nameEnd >= 0) {
            pkg = session.symbolProvider.getPackage(FqName.fromSegments(segments.subList(0, nameEnd - 1)))

            --nameEnd
        }

        if (pkg != null) {
            session.symbolProvider.getClassLikeSymbolByClassId(ClassId(pkg, FqName.fromSegments(segments.subList(nameEnd, segments.size)), false))?.let {
                return it
            }
        }

        session.symbolProvider.getClassLikeSymbolByClassId(ClassId(file.packageFqName, FqName(typeName), false))?.let {
            return it
        }

        for (import in file.imports) {
            if (import.isAllUnder) {
                val classId = ClassId(import.importedFqName!!, FqName.fromSegments(segments), false)
                session.symbolProvider.getClassLikeSymbolByClassId(classId)?.let { return it }

                continue
            }

            if (import.aliasName?.asString() == segments.first() || import.importedFqName?.asString() == segments.first()) {
                val importFqName = import.importedFqName ?: continue

                val classId = ClassId(importFqName.parent(), FqName.fromSegments(segments), false)

                session.symbolProvider.getClassLikeSymbolByClassId(classId)?.let { return it }
            }
        }

        return null
    }

    companion object {
        internal val PREDICATE = LookupPredicate.AnnotatedWith(setOf(AnnotationFqn(ClassExtension::class.java.name)))

        private val classExtensionAnnotation = classId(ClassExtension::class.java)

        internal fun classId(cls: Class<*>) = FqName(cls.name).let {
            ClassId(it.parent(), it.shortName())
        }
    }
}

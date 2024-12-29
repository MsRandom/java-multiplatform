package net.msrandom.classextensions.kotlin.plugin.copiers

import net.msrandom.classextensions.kotlin.plugin.ClassExtensionFirDeclarations.Key
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.buildBackingField
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessorCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyCopy
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

fun ConeKotlinType.replaceType(original: ClassId, replacement: ClassId): ConeClassLikeType {
    if (classId != original) {
        return this as ConeClassLikeType
    }

    return replacement.constructClassLikeType(typeArguments, isMarkedNullable, attributes)
}

@OptIn(UnresolvedExpressionTypeAccess::class)
fun copyProperty(
    original: FirProperty,
    ownerSymbol: FirClassSymbol<*>,
    callableId: CallableId,
): FirProperty {
    fun ConeKotlinType.replaceType() =
        replaceType(original.symbol.callableId.classId!!, callableId.classId!!)

    val propertyCopy = buildPropertyCopy(original) {
        symbol = FirPropertySymbol(callableId)
        origin = Key.origin
        moduleData = ownerSymbol.moduleData

        dispatchReceiverType = original.dispatchReceiverType?.replaceType()
    }

    val transformer = object : FirFunctionCopier(original.symbol, ownerSymbol) {
        override fun transformBackingField(backingField: FirBackingField, data: Unit): FirStatement {
            return super.transformBackingField(buildBackingField {
                source = backingField.source
                resolvePhase = backingField.resolvePhase
                moduleData = ownerSymbol.moduleData
                origin = Key.origin
                attributes = backingField.attributes
                returnTypeRef = backingField.returnTypeRef
                receiverParameter = backingField.receiverParameter
                deprecationsProvider = backingField.deprecationsProvider
                containerSource = backingField.containerSource
                dispatchReceiverType = backingField.dispatchReceiverType?.replaceType()
                contextReceivers.addAll(backingField.contextReceivers)
                name = backingField.name
                delegate = backingField.delegate
                isVar = backingField.isVar
                isVal = backingField.isVal
                getter = backingField.getter
                setter = backingField.setter
                this.backingField = backingField.backingField
                symbol = backingField.symbol
                propertySymbol = propertyCopy.symbol
                initializer = backingField.initializer
                annotations.addAll(backingField.annotations)
                typeParameters.addAll(backingField.typeParameters)
                status = backingField.status
            }, data)
        }

        override fun transformPropertyAccessor(
            propertyAccessor: FirPropertyAccessor,
            data: Unit
        ): FirPropertyAccessor {
            val copy = if (propertyAccessor is FirDefaultPropertyGetter) {
                FirDefaultPropertyGetter(
                    propertyAccessor.source,
                    ownerSymbol.moduleData,
                    Key.origin,
                    propertyAccessor.returnTypeRef,
                    propertyAccessor.visibility,
                    propertyCopy.symbol,
                    propertyAccessor.modality ?: Modality.FINAL,
                    propertyAccessor.effectiveVisibility,
                    propertyAccessor.isInline,
                    propertyAccessor.isOverride,
                    propertyAccessor.symbol,
                )
            } else if (propertyAccessor is FirDefaultPropertySetter) {
                FirDefaultPropertySetter(
                    propertyAccessor.source,
                    ownerSymbol.moduleData,
                    Key.origin,
                    propertyAccessor.returnTypeRef,
                    propertyAccessor.visibility,
                    propertyCopy.symbol,
                    propertyAccessor.modality ?: Modality.FINAL,
                    propertyAccessor.effectiveVisibility,
                    propertyAccessor.isInline,
                    propertyAccessor.isOverride,
                    propertyAccessor.symbol,
                    propertyAccessor.valueParameters.getOrNull(0)?.source,
                    propertyAccessor.valueParameters.getOrNull(0)?.annotations ?: emptyList(),
                )
            } else {
                buildPropertyAccessorCopy(propertyAccessor) {
                    origin = Key.origin
                    symbol = propertyAccessor.symbol
                    propertySymbol = propertyCopy.symbol
                    annotations.clear() // TODO only filter not-needed annotations, and fix their owner
                    // attributes = FirDeclarationAttributes()
                    dispatchReceiverType = propertyAccessor.dispatchReceiverType?.replaceType()
                }
            }

            return super.transformPropertyAccessor(copy, data) as FirPropertyAccessor
        }

        override fun <E : FirElement> transformElement(element: E, data: Unit) =
            element.transformChildren(this, Unit) as E
    }

    return propertyCopy.transformChildren(transformer, Unit) as FirProperty
}

package net.msrandom.classextensions.kotlin.plugin

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.ClassId

object ReferenceReplacerVisitor : FirVisitor<Unit, Pair<ClassId, ClassId>>() {
    override fun visitValueParameter(valueParameter: FirValueParameter, data: Pair<ClassId, ClassId>) {
        super.visitValueParameter(valueParameter, data)
    }

    override fun visitProperty(property: FirProperty, data: Pair<ClassId, ClassId>) {
        super.visitProperty(property, data)
    }

    override fun visitBackingField(backingField: FirBackingField, data: Pair<ClassId, ClassId>) {
        backingField.propertySymbol.callableId
        super.visitBackingField(backingField, data)
    }

    override fun visitElement(
        element: FirElement,
        data: Pair<ClassId, ClassId>
    ) {
        println(element)
    }
}

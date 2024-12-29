package net.msrandom.classextensions.kotlin.plugin

import net.msrandom.classextensions.ClassExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.classFqName

class ExcludeClassExtensionsIrGenerationExtension : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        for (file in moduleFragment.files) {
            file.declarations.removeAll {
                it is IrClass && it.annotations.any {
                    it.type.classFqName.toString() == ClassExtension::class.java.name
                }
            }
        }
    }
}

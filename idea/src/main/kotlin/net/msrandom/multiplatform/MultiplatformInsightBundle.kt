package net.msrandom.multiplatform

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val MULTIPLATFORM_INSIGHT_BUNDLE = "messages.MultiplatformInsightBundle"

object MultiplatformInsightBundle : DynamicBundle(MULTIPLATFORM_INSIGHT_BUNDLE) {
    fun message(key: @PropertyKey(resourceBundle = MULTIPLATFORM_INSIGHT_BUNDLE) String, vararg props: Any?): @Nls String {
        return if (containsKey(key)) {
            getMessage(key, *props)
        } else {
            key
        }
    }
}

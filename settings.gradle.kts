enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "java-multiplatform"

fun includeSubproject(name: String, path: String) {
    include(name)

    project(":$name").projectDir = file(path)
}

includeSubproject("java-virtual-source-sets-idea", "virtual-source-sets/idea-plugin")
includeSubproject("java-virtual-source-sets", "virtual-source-sets/gradle-plugin")

includeSubproject( "java-expect-actual-idea", "java-expect-actual/idea-plugin")
includeSubproject("java-expect-actual-annotations", "java-expect-actual/annotations")
includeSubproject("java-expect-actual-processor", "java-expect-actual/processor")

includeSubproject("kmp-stub-annotations", "kmp-stubs/annotations")
includeSubproject("kmp-stubs-processor", "kmp-stubs/processor")

includeSubproject("class-extension-idea", "class-extensions/idea-plugin")
includeSubproject("class-extension-annotations", "class-extensions/annotations")
includeSubproject("class-extension-java-processor", "class-extensions/java-processor")
includeSubproject("class-extension-kotlin-plugin", "class-extensions/kotlin-plugin")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "java-multiplatform"

fun includeSubproject(name: String, path: String) {
    include(name)

    project(":$name").projectDir = file(path)
}

includeSubproject("java-virtual-source-sets-idea", "virtual-source-sets/idea")
includeSubproject("java-virtual-source-sets", "virtual-source-sets/gradle-plugin")

includeSubproject( "java-expect-actual-idea", "expect-actual/idea")
includeSubproject("java-expect-actual-annotations", "expect-actual/annotations")
includeSubproject("java-expect-actual-processor", "expect-actual/processor")

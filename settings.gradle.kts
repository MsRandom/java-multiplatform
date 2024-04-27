enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "multiplatform"

fun includeSubModules(vararg names: String) {
    include(*names)

    for (name in names) {
        project(":$name").name = "${rootProject.name}-$name"
    }
}

includeSubModules(
    "annotations",
    "processor",
    "test-project",
)

include("idea")

project(":idea").let {
    it.name = "java-${rootProject.name}-${it.name}"
}

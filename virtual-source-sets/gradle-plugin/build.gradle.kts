plugins {
    java
    kotlin("jvm")
    id("com.google.devtools.ksp")
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    plugins.create("javaVirtualSourceSets") {
        id = project.name

        displayName = "Java Virtual Source Sets"

        description =
            "A plugin that allows including a source set for compilation with another, " +
            "rather than compiling separately and depending on each other"

        implementationClass = "net.msrandom.virtualsourcesets.JavaVirtualSourceSetsPlugin"
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    ksp(group = "dev.zacsweers.autoservice", name = "auto-service-ksp", version = "1.2.0")
    compileOnly(group = "com.google.auto.service", name = "auto-service-annotations", version = "1.1.1")
    api(group = "dev.gradleplugins", name = "gradle-api", version = "8.2")

    compileOnly(kotlin("gradle-plugin"))
}

publishing {
    repositories {
        mavenLocal()

        maven("https://maven.msrandom.net/repository/root/") {
            credentials {
                val mavenUsername: String? by project
                val mavenPassword: String? by project

                username = mavenUsername
                password = mavenPassword
            }
        }
    }
}

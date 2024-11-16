plugins {
    java
    kotlin("jvm")
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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij")
    groovy
    `maven-publish`
}

repositories {
    mavenCentral()
}

val gradleToolingExtension: SourceSet by sourceSets.creating

val gradleToolingExtensionJar = tasks.register(gradleToolingExtension.jarTaskName, Jar::class) {
    from(gradleToolingExtension.output)

    archiveClassifier.set(gradleToolingExtension.name)
}

tasks.named(gradleToolingExtension.getCompileTaskName("groovy"), GroovyCompile::class) {
    classpath += files(gradleToolingExtension.kotlin.destinationDirectory)
}

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases/")
}

dependencies {
    gradleToolingExtension.implementationConfigurationName(kotlin("stdlib"))

    gradleToolingExtension.compileOnlyConfigurationName(group = "com.jetbrains.intellij.gradle", name = "gradle-tooling-extension", version = "241.18034.82") {
        exclude("org.jetbrains.intellij.deps", "gradle-api")
    }

    implementation(files(gradleToolingExtensionJar))
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2024.1")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("gradle", "java"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("251.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.jetbrains.plugins"
            artifactId = "net.msrandom.java-virtual-sourcesets"
            version = project.version.toString()

            artifact(tasks.buildPlugin)
        }
    }
}

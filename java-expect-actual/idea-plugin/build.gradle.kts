plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij")
}

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  implementation(projects.javaExpectActualAnnotations)
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2024.1")
  type.set("IC") // Target IDE Platform

  plugins.set(listOf("gradle", "java"/*, "net.msrandom.java-virtual-sourcesets:1.0.0"*/))
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
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

plugins {
    `java-library`
    `maven-publish`
}

version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

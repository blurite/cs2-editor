plugins {
    `maven-publish`
    kotlin("jvm") version "1.8.0"
}

dependencies {
    implementation("com.displee:disio:2.2")
    implementation("com.displee:rs-cache-library:6.8.1")
    implementation("org.apache.commons:commons-lang3:3.12.0")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/blurite/cs2-editor")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}

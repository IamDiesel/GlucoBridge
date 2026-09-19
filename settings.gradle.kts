pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GlucoBridge"

include(":app")
include(":wear")
include(":core:model")
include(":core:glucose-api")
include(":domain")
include(":data:source-rest")
include(":data:repository")

// IP-Zone (git-ignoriert): nur einbinden, wenn lokal vorhanden -> absent-safe Build (IP-4).
if (file("data/source-ale/build.gradle.kts").exists()) {
    include(":data:source-ale")
}

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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "android-java-lab"

// O módulo :core é Java puro e não depende do Android SDK.
// Use `./gradlew -PcoreOnly test` para rodar os engines em qualquer máquina com JDK 17+.
include(":core")
if (!providers.gradleProperty("coreOnly").isPresent) {
    include(":app")
}

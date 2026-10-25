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
        google()
        mavenCentral()
    }
}

rootProject.name = "openRun"
// T0: throwaway technical spike (docs/MVP.md).
include(":spike")
// T1: the logic that decides things (programs, progress, calories, ranks, workout clock). Pure Kotlin, no Android.
include(":core")
// The app itself. T2 adds its storage (Room) and settings (DataStore); the screens come in T3.
include(":app")

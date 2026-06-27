pluginManagement {
    repositories {
        google()
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

rootProject.name = "ArkeoScanPhone"

include(
    ":app",
    ":core-common",
    ":core-database",
    ":core-gps",
    ":core-motion",
    ":core-magnetometer",
    ":core-analysis",
    ":core-renderer",
    ":core-camera",
    ":core-reports"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FastQrScan"

include(":app")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":core:common")
include(":feature:scanner")
include(":feature:generator")
include(":feature:history")
include(":feature:ai-workspace")
include(":feature:onboarding")
include(":feature:product-lookup")

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

rootProject.name = "LexoraService"
include(":app")
include(":core:designsystem")
include(":core:model")
include(":core:navigation")
include(":core:data")
include(":core:domain")
include(":core:database")
include(":feature:home")
include(":feature:settings")
include(":feature:wash")
include(":feature:tires")
include(":feature:clients")
include(":feature:vehicles")
include(":feature:assets")
include(":feature:organization")
include(":feature:requests")

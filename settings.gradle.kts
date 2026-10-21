pluginManagement {
    repositories {
        google()
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

rootProject.name = "LexoraService"
include(":app")
include(":core:designsystem")
include(":core:model")
include(":core:navigation")
include(":core:data")
include(":core:domain")
include(":core:database")
include(":core:network")
include(":core:presentation")
include(":core:di")
include(":feature:home")
include(":feature:planning")
include(":feature:search")
include(":feature:settings")
include(":feature:wash")
include(":feature:tires")
include(":feature:clients")
include(":feature:vehicles")
include(":feature:assets")
include(":feature:organization")
include(":feature:requests")
include(":feature:fieldwork")
include(":feature:documents")
include(":feature:reports")
include(":feature:catalog")
include(":feature:notifications")
include(":feature:audit")
include(":feature:users")

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.lexora.service.core.domain"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}
dependencies {
    implementation(project(":core:model"))
}

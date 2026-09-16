plugins {
    id("com.android.library")
}

android {
    namespace = "com.lexora.service.core.network"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(project(":core:model"))
}

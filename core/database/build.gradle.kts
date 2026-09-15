plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.lexora.service.core.database"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}
dependencies {
    implementation(project(":core:model"))
    api("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
}

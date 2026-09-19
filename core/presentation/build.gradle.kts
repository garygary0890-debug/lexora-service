plugins {
    id("com.android.library")
}

android {
    namespace = "com.lexora.service.core.presentation"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
}

dependencies {
    api("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
}


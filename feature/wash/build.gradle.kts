plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.lexora.service.feature.wash"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":core:model"))
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
}

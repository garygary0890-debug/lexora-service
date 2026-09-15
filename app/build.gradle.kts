plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.lexora.service"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lexora.service"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "0.9.0"
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":feature:fieldwork"))
    implementation(project(":feature:requests"))
    implementation(project(":feature:organization"))
    implementation(project(":feature:assets"))
    implementation(project(":feature:vehicles"))
    implementation(project(":feature:clients"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":feature:home"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:wash"))
    implementation(project(":feature:tires"))

    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.navigation:navigation-compose:2.9.3")
}

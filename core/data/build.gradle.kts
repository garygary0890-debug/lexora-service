plugins {
    id("com.android.library")
}
android {
    namespace = "com.lexora.service.core.data"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
}

plugins {
    id("com.android.library")
}
android {
    namespace = "com.lexora.service.core.domain"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}
dependencies {
    implementation(project(":core:model"))
    testImplementation("junit:junit:4.13.2")
}

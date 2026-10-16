plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "app.ciying.keyboard.test"
    compileSdk = 35
    compileSdkExtension = 15
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "app.ciying.keyboard.test"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}


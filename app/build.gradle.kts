import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val signingPropertiesFile = providers.gradleProperty("ciyingSigningProperties").orNull
    ?.let { rootProject.file(it) }
    ?: providers.environmentVariable("CIYING_SIGNING_PROPERTIES").orNull?.let { file(it) }
    ?: rootProject.rootDir.parentFile.resolve("Ciying-Signing-Backup/signing.properties")
val releaseCredentials = Properties().apply {
    if (signingPropertiesFile.isFile) signingPropertiesFile.inputStream().use { load(it) }
}
val hasReleaseCredentials = signingPropertiesFile.isFile

android {
    namespace = "app.ciying.keyboard"
    compileSdk = 35
    compileSdkExtension = 15
    buildToolsVersion = "35.0.0"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        applicationId = "app.ciying.keyboard"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.3.3"
    }

    signingConfigs {
        if (hasReleaseCredentials) {
            create("ciyingRelease") {
                storeFile = signingPropertiesFile.parentFile.resolve(releaseCredentials.getProperty("storeFile"))
                storePassword = releaseCredentials.getProperty("storePassword")
                keyAlias = releaseCredentials.getProperty("keyAlias")
                keyPassword = releaseCredentials.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }
    buildTypes {
        getByName("release") {
            if (hasReleaseCredentials) signingConfig = signingConfigs.getByName("ciyingRelease")
        }
    }
}

gradle.taskGraph.whenReady {
    if (allTasks.any { it.project == project && it.name.contains("Release") } && !hasReleaseCredentials) {
        throw GradleException("Release signing configuration missing. Set CIYING_SIGNING_PROPERTIES to your private signing.properties file.")
    }
}

dependencies { testImplementation("junit:junit:4.13.2") }

import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val isSigningConfigured: Boolean = if (keystorePropertiesFile.exists() && keystorePropertiesFile.isFile) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    val storeFilePath = keystoreProperties.getProperty("storeFile")?.trim()
    val storePassword = keystoreProperties.getProperty("storePassword")?.trim()
    val keyAlias = keystoreProperties.getProperty("keyAlias")?.trim()
    val keyPassword = keystoreProperties.getProperty("keyPassword")?.trim()

    val hasAllRequiredProperties = !storeFilePath.isNullOrBlank() &&
        !storePassword.isNullOrBlank() &&
        !keyAlias.isNullOrBlank() &&
        !keyPassword.isNullOrBlank()

    if (hasAllRequiredProperties) {
        val targetFile = File(storeFilePath!!)
        val resolvedStoreFile = if (targetFile.isAbsolute) targetFile else rootProject.file(storeFilePath)
        resolvedStoreFile.exists() && resolvedStoreFile.isFile
    } else {
        false
    }
} else {
    false
}

android {
    namespace = "com.brailuxaprende"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.brailuxaprende"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (isSigningConfigured) {
            create("release") {
                val storeFilePath = keystoreProperties.getProperty("storeFile").trim()
                val targetFile = File(storeFilePath)
                storeFile = if (targetFile.isAbsolute) targetFile else rootProject.file(storeFilePath)
                storePassword = keystoreProperties.getProperty("storePassword").trim()
                keyAlias = keystoreProperties.getProperty("keyAlias").trim()
                keyPassword = keystoreProperties.getProperty("keyPassword").trim()
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (isSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.google.play.billing.ktx)
    implementation(libs.firebase.ai)
    debugImplementation(libs.firebase.appcheck.debug)
    releaseImplementation(libs.firebase.appcheck.playintegrity)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

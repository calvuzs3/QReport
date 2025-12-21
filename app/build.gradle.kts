// QReport - build.gradle.kts (app level)
// Versione: 1.0 - Ottobre 2025
// Namespace: net.calvuz.qreport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "net.calvuz.qreport"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.calvuz.qreport"
        minSdk = 33
        //noinspection OldTargetApi
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // ✅ AGGIORNATO test runner per Hilt
        testInstrumentationRunner = "net.calvuz.qreport.CustomTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            manifestPlaceholders["appName"] = "QReport Debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["appName"] = "QReport"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // ✅ AGGIUNTO per test configuration
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.exifinterface)
    ksp(libs.androidx.room.compiler)

    // Dependency Injection - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // CameraX for photo capture
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    // Apache POI for Word export
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)
    implementation(libs.apache.poi.scratchpad)

    // Image Loading - Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // JSON Serialization
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)

    // Date/Time
    implementation(libs.kotlinx.datetime)

    // Logging
    implementation(libs.timber)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Datastore
    implementation(libs.datastore.preferences)

    // ===== TESTING DEPENDENCIES =====

    // Unit Tests
    testImplementation(libs.junit)
//    testImplementation(libs.kotlinx.test)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    // ✅ HILT TESTING - Unit Tests
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.android.compiler)

    // ✅ ROBOLECTRIC - Unit Tests
    testImplementation(libs.robolectric)

    // Android Instrumented Tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // ✅ HILT TESTING - Instrumented Tests
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}
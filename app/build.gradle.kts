plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "de.glucobridge.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "de.glucobridge.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Projekt-Module ---
    implementation(project(":domain"))
    implementation(project(":core:model"))
    implementation(project(":core:glucose-api"))
    implementation(project(":data:repository"))
    implementation(project(":data:source-rest"))
    // IP-Zone (git-ignoriert): nur wenn lokal vorhanden -> absent-safe (IP-4)
    if (rootProject.file("data/source-ale/build.gradle.kts").exists()) {
        implementation(project(":data:source-ale"))
    }

    // --- Async ---
    implementation(libs.kotlinx.coroutines.android)

    // --- Wear OS (Data Layer, Handy-seitiger Push) ---
    implementation(libs.play.services.wearable)

    // --- Sicherheit (verschluesselte Session) ---
    implementation(libs.androidx.security.crypto)

    // --- Export: Health Connect + HTTP (Nightscout) ---
    implementation(libs.androidx.health.connect)
    implementation(libs.okhttp)

    // --- Persistenz: Verlauf (Room, KSP) ---
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // --- Compose / Lifecycle ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // --- Tests ---
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

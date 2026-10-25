plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.digitalminds.beej"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.digitalminds.beej"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // Solo estas dos: cubre el Galaxy A03 Core (32 bits) y los teléfonos modernos (64 bits).
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
    }

    signingConfigs {
        // Misma firma en debug y release para poder actualizar encima de versiones anteriores.
        create("beej") {
            storeFile = file("beej-release.jks")
            storePassword = "beej2026ks"
            keyAlias = "beej"
            keyPassword = "beej2026ks"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("beej")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("beej")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    androidResources {
        // openFd() solo funciona con assets sin comprimir.
        noCompress += listOf("ogg")
    }

    packaging {
        jniLibs { useLegacyPackaging = true } // librerías nativas comprimidas: APK más liviano
        resources { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/*.version") }
    }

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // Reconocimiento de voz offline (gramática cerrada).
    implementation("com.alphacephei:vosk-android:0.3.47")
    implementation("net.java.dev.jna:jna:5.13.0@aar")

    testImplementation("junit:junit:4.13.2")
}

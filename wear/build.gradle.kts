plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.customvibrationnotifier"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myvibrationproject"
        minSdk = 30   // Wear OS 3 以上
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.wear:wear:1.3.0")

    // ★ 時計側のメッセージ受信に必要
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
}

@file:Suppress("UnstableApiUsage")

import com.jason.cloud.buildsrc.Android
import com.jason.cloud.buildsrc.Dependencies

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jason.cloud.extension"
    compileSdk = Android.compileSdk

    defaultConfig {
        minSdk = Android.minSdk
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation("androidx.core:core-ktx:${Dependencies.androidx_core_ktx}")
    implementation("androidx.appcompat:appcompat:${Dependencies.androidx_appcompat}")
    implementation("com.google.android.material:material:${Dependencies.google_material}")
    implementation("com.github.bumptech.glide:glide:${Dependencies.glide}")
    ksp("com.github.bumptech.glide:ksp:${Dependencies.glide}")
    implementation("com.tencent:mmkv:${Dependencies.mmkv}")
}
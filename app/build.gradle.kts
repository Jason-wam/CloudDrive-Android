@file:Suppress("UnstableApiUsage")

import com.jason.cloud.buildsrc.Android
import com.jason.cloud.buildsrc.Dependencies

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jason.cloud.drive"
    compileSdk = Android.compileSdk

    defaultConfig {
        applicationId = "com.jason.cloud.drive"
        minSdk = Android.minSdk
        targetSdk = Android.targetSdk
        versionCode = Android.versionCode
        versionName = Android.versionName

        ndk.abiFilters.add("arm64-v8a")
        ndk.abiFilters.add("armeabi-v7a")

        vectorDrawables {
            useSupportLibrary = true
        }

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildFeatures {
        dataBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    packaging {
        resources.excludes.add("META-INF/beans.xml")
        resources.excludes.add("META-INF/LICENSE.txt")
        resources.excludes.add("about_files/**")
    }
}

dependencies {
    implementation(project(mapOf("path" to ":theme")))
    implementation(project(mapOf("path" to ":extension")))
    implementation(project(mapOf("path" to ":media3")))
    implementation(project(mapOf("path" to ":cling")))

    implementation("androidx.core:core-ktx:${Dependencies.androidx_core_ktx}")
    implementation("androidx.appcompat:appcompat:${Dependencies.androidx_appcompat}")
    implementation("com.google.android.material:material:${Dependencies.google_material}")
    implementation("androidx.constraintlayout:constraintlayout:${Dependencies.androidx_constraintlayout}")

    ksp("androidx.room:room-compiler:${Dependencies.androidx_room}")
    implementation("androidx.room:room-ktx:${Dependencies.androidx_room}")
    implementation("androidx.room:room-runtime:${Dependencies.androidx_room}")

    implementation("androidx.legacy:legacy-support-v4:${Dependencies.androidx_legacy_support_v4}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Dependencies.androidx_lifecycle}")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${Dependencies.androidx_lifecycle}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Dependencies.androidx_lifecycle}")

    //网络相关
    implementation("com.qiniu:happy-dns:${Dependencies.qiniu_dns}")
    implementation("com.squareup.okhttp3:okhttp:${Dependencies.okhttp3}")

    implementation("com.github.liangjingkanji:Net:${Dependencies.net}")
    implementation("com.github.liangjingkanji:spannable:${Dependencies.spannable}")
    implementation("com.github.liangjingkanji:soft-input-event:${Dependencies.soft_input_event}")

    implementation("com.github.bumptech.glide:glide:${Dependencies.glide}")
    implementation("com.github.bumptech.glide:okhttp3-integration:${Dependencies.glide}")

    implementation("com.github.getActivity:XXPermissions:${Dependencies.xx_permissions}")
    implementation("com.drakeet.multitype:multitype:${Dependencies.multiType}")

    //状态栏工具
    implementation("com.geyifeng.immersionbar:immersionbar:${Dependencies.immersionbar}")
    implementation("com.geyifeng.immersionbar:immersionbar-ktx:${Dependencies.immersionbar}")

    implementation("io.github.scwang90:refresh-layout-kernel:${Dependencies.smart_refresh}")
    implementation("io.github.scwang90:refresh-header-material:${Dependencies.smart_refresh}")

    implementation("io.github.FlyJingFish.OpenImage:OpenImageGlideLib:${Dependencies.open_image}")
}
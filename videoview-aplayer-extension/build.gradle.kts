import com.jason.cloud.buildsrc.Android
import com.jason.cloud.buildsrc.Dependencies

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jason.aplaye.extension"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    compileOnly(project(mapOf("path" to ":videoview")))
    implementation(files("libs/APlayerAndroid_1.2.3.499.aar"))
    implementation("androidx.core:core-ktx:${Dependencies.androidx_core_ktx}")
}
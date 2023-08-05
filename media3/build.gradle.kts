import com.jason.cloud.buildsrc.Dependencies

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jason.cloud.media3"
    compileSdk = 33

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation("androidx.core:core-ktx:${Dependencies.androidx_core_ktx}")
    implementation("androidx.appcompat:appcompat:${Dependencies.androidx_appcompat}")
    implementation("com.google.android.material:material:${Dependencies.google_material}")

    // For media playback using ExoPlayer
    implementation("androidx.media3:media3-exoplayer:${Dependencies.media3}")
    // For DASH playback support with ExoPlayer
    implementation("androidx.media3:media3-exoplayer-dash:${Dependencies.media3}")
    // For HLS playback support with ExoPlayer
    implementation("androidx.media3:media3-exoplayer-hls:${Dependencies.media3}")
    // For RTSP playback support with ExoPlayer
    implementation("androidx.media3:media3-exoplayer-rtsp:${Dependencies.media3}")

    // For loading data using the Cronet network stack
    implementation("androidx.media3:media3-datasource-cronet:${Dependencies.media3}")
    // For loading data using the OkHttp network stack
    implementation("androidx.media3:media3-datasource-okhttp:${Dependencies.media3}")
    // For loading data using librtmp
    implementation("androidx.media3:media3-datasource-rtmp:${Dependencies.media3}")

    // For building media playback UIs
    implementation("androidx.media3:media3-ui:${Dependencies.media3}")
    // For building media playback UIs for Android TV using the Jetpack Leanback library
    implementation("androidx.media3:media3-ui-leanback:${Dependencies.media3}")

    // For exposing and controlling media sessions
    implementation("androidx.media3:media3-session:${Dependencies.media3}")

    // For extracting data from media containers
    implementation("androidx.media3:media3-extractor:${Dependencies.media3}")

    // Common functionality for media decoders
    implementation("androidx.media3:media3-decoder:${Dependencies.media3}")
    // Common functionality for loading data
    implementation("androidx.media3:media3-datasource:${Dependencies.media3}")
    // Common functionality used across multiple media libraries
    implementation("androidx.media3:media3-common:${Dependencies.media3}")

    // For integrating with Cast
    // implementation("androidx.media3:media3-cast:${Dependencies.media3}")

    // For scheduling background operations using Jetpack Work's WorkManager with ExoPlayer
    // implementation("androidx.media3:media3-exoplayer-workmanager:${Dependencies.media3}")

    // For transforming media files
    // implementation("androidx.media3:media3-transformer:${Dependencies.media3}")

    // Utilities for testing media components (including ExoPlayer components)
    // implementation("androidx.media3:media3-test-utils:${Dependencies.media3}")
    // Utilities for testing media components (including ExoPlayer components) via Robolectric
    // implementation("androidx.media3:media3-test-utils-robolectric:${Dependencies.media3}")

    // Common functionality for media database components
    // implementation("androidx.media3:media3-database:${Dependencies.media3}")
    // implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // For ad insertion using the Interactive Media Ads SDK with ExoPlayer
    // implementation("androidx.media3:media3-exoplayer-ima:${Dependencies.media3}")
}
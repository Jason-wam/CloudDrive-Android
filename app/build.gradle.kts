@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jason.cloud.drive"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.jason.cloud.drive"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
    buildFeatures { //DataBinding必须依赖KAPT
        dataBinding = true
    }
    signingConfigs {
        getByName("debug") {
            keyAlias = "key0"
            keyPassword = "mmmm2521"
            storeFile = file("$projectDir/CoolApk.jks")
            storePassword = "mmmm2521"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
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
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    val roomVersion = "2.4.2"
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-runtime:$roomVersion")

    //网络相关
    implementation("com.qiniu:happy-dns:2.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.github.liangjingkanji:Net:3.5.8")
    implementation("com.github.liangjingkanji:spannable:1.2.6")
    implementation("com.github.liangjingkanji:soft-input-event:1.0.9")

    implementation("com.github.getActivity:XXPermissions:16.8")
    implementation("com.github.bumptech.glide:glide:4.15.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.15.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    ksp("com.github.bumptech.glide:ksp:4.15.0")

    //状态栏工具
    implementation("com.geyifeng.immersionbar:immersionbar:3.2.2")
    implementation("com.geyifeng.immersionbar:immersionbar-ktx:3.2.2")

    //多类型RecyclerView布局
    implementation("com.drakeet.multitype:multitype:4.3.0")

    implementation("io.github.scwang90:refresh-layout-kernel:2.0.5")
    implementation("io.github.scwang90:refresh-header-material:2.0.5")

    implementation("io.github.youth5201314:banner:2.2.2")
    implementation("io.github.jeremyliao:live-event-bus-x:1.8.0")
    implementation("io.github.FlyJingFish.OpenImage:OpenImageGlideLib:2.1.0")

    implementation("com.tencent:mmkv:1.2.15")
}
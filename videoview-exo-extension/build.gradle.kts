@file:Suppress("UnstableApiUsage")

import com.jason.cloud.buildsrc.Android
import com.jason.cloud.buildsrc.Dependencies

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.jason.exo.extension"
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
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    compileOnly(project(mapOf("path" to ":videoview")))
    implementation("androidx.core:core-ktx:${Dependencies.androidx_core_ktx}")

    implementation("com.google.android.exoplayer:exoplayer-core:${Dependencies.exo}")
    implementation("com.google.android.exoplayer:exoplayer-dash:${Dependencies.exo}")
    implementation("com.google.android.exoplayer:exoplayer-hls:${Dependencies.exo}")
    implementation("com.google.android.exoplayer:exoplayer-rtsp:${Dependencies.exo}")
    implementation("com.google.android.exoplayer:extension-rtmp:${Dependencies.exo}")
    implementation("com.google.android.exoplayer:exoplayer-smoothstreaming:${Dependencies.exo}")
}

// 创建一个task来发布源码
tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    val sources = android.sourceSets.map { set -> set.java.getSourceFiles() }
    from(sources)
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                url = uri("D:/Maven")
            }
        }
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["release"])
                groupId = "com.jason"
                artifactId = "videoview-exo-extension"
                version = "1.0.0"
                System.out.println("implementation(\"$groupId:$artifactId:$version\")")
            }
        }
    }
}
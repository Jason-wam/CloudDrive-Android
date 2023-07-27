@file:Suppress("UnstableApiUsage")

import com.jason.cloud.buildsrc.Android
import com.jason.cloud.buildsrc.Dependencies

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.jason.videoview"
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
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:${Dependencies.androidx_core_ktx}")
    implementation("androidx.appcompat:appcompat:${Dependencies.androidx_appcompat}")
    implementation("com.google.android.material:material:${Dependencies.google_material}")

    implementation(project(mapOf("path" to ":theme")))
    implementation(project(mapOf("path" to ":extension")))
//    implementation(project(mapOf("path" to ":videoview-exo-extension")))
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
                artifactId = "videoview"
                version = "1.0.0"
                System.out.println("implementation(\"$groupId:$artifactId:$version\")")
            }
        }
    }
}
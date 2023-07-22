@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.jason.videoview"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
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
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")

    implementation(project(mapOf("path" to ":theme")))
    implementation(project(mapOf("path" to ":extension")))
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
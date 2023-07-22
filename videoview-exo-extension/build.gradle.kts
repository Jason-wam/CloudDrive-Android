@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.jason.exo.extension"
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
    compileOnly(project(mapOf("path" to ":videoview")))

    val exoVersion = "2.18.4"
    api("com.google.android.exoplayer:exoplayer-core:$exoVersion")
    api("com.google.android.exoplayer:exoplayer-dash:$exoVersion")
    api("com.google.android.exoplayer:exoplayer-hls:$exoVersion")
    api("com.google.android.exoplayer:exoplayer-rtsp:$exoVersion")
    api("com.google.android.exoplayer:exoplayer-smoothstreaming:$exoVersion")
    api("com.google.android.exoplayer:extension-rtmp:$exoVersion")
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
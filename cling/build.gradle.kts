@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.jason.cast"
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.0")

    //noinspection GradleDependency
    api("org.fourthline.cling:cling-core:2.1.1")
    //noinspection GradleDependency
    api("org.fourthline.cling:cling-support:2.1.1")

    implementation(files("libs\\javax.servlet-3.0.0.v201103241009.jar"))
    implementation(files("libs\\jetty-client-8.1.9.v20130131.jar"))
    implementation(files("libs\\jetty-continuation-8.1.9.v20130131.jar"))
    implementation(files("libs\\jetty-http-8.1.9.v20130131.jar"))
    implementation(files("libs\\jetty-io-8.1.9.v20130131.jar"))
    implementation(files("libs\\jetty-security-8.1.9.v20130131.jar"))
    implementation(files("libs\\jetty-server-8.1.9.v20130131.jar"))
    implementation(files("libs\\jetty-servlet-8.1.9.v20130131.jar"))
    implementation(files("libs\\jetty-util-8.1.9.v20130131.jar"))
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
                artifactId = "cling"
                version = "1.0.0"
                System.out.println("implementation(\"$groupId:$artifactId:$version\")")
            }
        }
    }
}
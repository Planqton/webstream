plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    viewBinding {
        enable = true
    }
    namespace = "at.plankt0n.webstream"
    compileSdk = 35

    defaultConfig {
        applicationId = "at.plankt0n.webstream"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "android"
            keyPassword = "android"
            storeFile = file("../platform.jks")
            storePassword = "android"        }
        create("release") {
            keyAlias = "android"
            keyPassword = "android"
            storeFile = file("../platform.jks")
            storePassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation ("androidx.core:core:1.15.0")

    implementation ("androidx.media3:media3-extractor:1.5.1")
    implementation ("androidx.media3:media3-exoplayer:1.5.1")
    implementation ("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation ("androidx.media3:media3-session:1.5.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
}
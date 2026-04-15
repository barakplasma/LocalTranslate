plugins {
    id("com.android.application") version "9.1.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
    id("org.jetbrains.kotlin.kapt") version "2.3.20"
}

android {
    compileSdk = 36

    defaultConfig {
        applicationId = "com.barakplasma.privateaitranslate"
        minSdk = 26
        targetSdk = 35
        versionCode = 58
        versionName = "19.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    flavorDimensions += "internet"
    productFlavors {
        create("full") {
            dimension = "internet"
            buildConfigField("Boolean", "ON_DEVICE_ONLY", "false")
        }
        create("noInternet") {
            dimension = "internet"
            buildConfigField("Boolean", "ON_DEVICE_ONLY", "true")
            versionNameSuffix = "-offline"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    namespace = "com.barakplasma.privateaitranslate"
}

dependencies {
    val compose_version: String by rootProject.extra
    // Android Core
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Compose
    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-tooling:$compose_version")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose_version")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("com.materialkolor:material-kolor:3.0.1")

    // Retrofit & API
    implementation(project(":translation-engines"))
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    // retrofit:3.0.0 based on okhttp3:4.12.0

    // Room database
    implementation("androidx.room:room-ktx:2.7.2")
    kapt("androidx.room:room-compiler:2.7.2")

    // Tesseract OCR
    implementation("cz.adaptech.tesseract4android:tesseract4android-openmp:4.9.0")

    // Dynamic color scheme
    implementation("com.google.android.material:material:1.12.0")

    // Gemini Nano on-device AI via ML Kit GenAI Prompt API
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta1")

    // LiteRT-LM on-device LLM inference
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.10.0")

    // ML Kit Translation
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Sentry for crash reporting
    implementation("io.sentry:sentry-android-core:7.14.0")
}

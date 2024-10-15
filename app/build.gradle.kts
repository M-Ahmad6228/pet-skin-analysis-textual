
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id ("com.chaquo.python")
}

android {
    namespace = "com.example.petcare"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.petcare"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            // On Apple silicon, you can omit x86_64.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2" // Updated to latest stable version of Compose
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

chaquopy {
    sourceSets {
        getByName("main") {
            srcDir("src/main/python")
        }
    }


    defaultConfig {
        buildPython("C:/Users/ahmad.muzaffar/AppData/Local/Programs/Python/Python312/python.exe")

        pip {
            // A requirement specifier, with or without a version number:
            install("scikit-learn")
        }
    }
}

dependencies {
    // Core Android libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose libraries
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui:1.5.3") // Updated to latest stable version
    implementation("androidx.compose.ui:ui-tooling:1.5.3") // Updated tooling version
    implementation("androidx.compose.material3:material3:1.1.0")  // Material 3 design system

    // TensorFlow Lite for model inference
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.0")  // For tokenization support
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.13.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.json:json:20210307")

    // Apache Commons CSV for reading CSV files
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("com.opencsv:opencsv:5.6")
//    implementation("com.chaquo.python:gradle:15.0.1")// Add this line


    // Confirm if 'litert' is required and replace with the appropriate dependency if not.
    // implementation(libs.litert) // Ensure this exists or replace/remove as necessary.

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debugging tools for Compose
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

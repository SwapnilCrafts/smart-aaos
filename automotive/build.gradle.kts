plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.swapnil.smart.aaos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.swapnil.smart.aaos"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        aidl = true
    }
    useLibrary("android.car")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.media)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.car.app:app:1.4.0")
    implementation("androidx.car.app:app-automotive:1.4.0")
    implementation("androidx.media:media:1.7.0")

    implementation("androidx.media3:media3-exoplayer:1.3.0")
    implementation("androidx.media3:media3-session:1.3.0")
    implementation("androidx.media3:media3-common:1.3.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
}
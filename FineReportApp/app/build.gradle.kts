plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.keboda.finereport"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.keboda.finereport"
        minSdk = 21
        targetSdk = 35
        versionCode = 11
        versionName = "1.1"
        multiDexEnabled = true
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.java.websocket)
    implementation(libs.gson)
    implementation(libs.okhttp)
}
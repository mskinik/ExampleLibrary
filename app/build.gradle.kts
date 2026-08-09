plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mskinik.examplelibrary"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mskinik.examplelibrary"
        minSdk = 24
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
}

dependencies {

    // BoM POC: platform(project(":bom")) here demonstrates the mechanism using
    // an in-repo module dependency. External consumer apps would instead write
    // implementation(platform("com.github.mskinik:examplelibrary-bom:1.0.0"))
    // and pull artifacts via GitHub Packages without a version number, as
    // shown below for MyModelLibrary and stringutils.
    implementation(platform(project(":bom")))
    implementation(project(":stringutils"))
    implementation("com.github.mskinik:MyModelLibrary")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
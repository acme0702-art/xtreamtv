plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.xtreamtv"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.xtreamtv"
        minSdk = 21
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
        ndk { abiFilters += listOf("armeabi-v7a") } // 32 bits
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
}

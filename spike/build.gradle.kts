plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.openrun.spike"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.openrun.spike"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

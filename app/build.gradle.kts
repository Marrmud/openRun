import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Where the release key comes from: the environment (CI), else a local keystore.properties (never committed).
// Without either, the release build still works but is left unsigned.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(env: String, key: String): String? = System.getenv(env) ?: keystoreProps.getProperty(key)

android {
    namespace = "org.openrun.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.openrun.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val storeFile = signingValue("OPENRUN_KEYSTORE_FILE", "storeFile")
        if (storeFile != null) {
            create("release") {
                this.storeFile = file(storeFile)
                storePassword = signingValue("OPENRUN_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("OPENRUN_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("OPENRUN_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // lives next to the real app on the same phone, with its own data
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// The database schema is committed (app/schemas) so a change that would lose data cannot slip in unnoticed.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.health.connect)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kspPlugin)
}

// Secrets live in local.properties, which is not committed to GitHub.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.dante.sportshuub"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.dante.sportshuub"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Free by default: Groq's OpenAI-compatible server.
        buildConfigField(
            "String",
            "AI_API_KEY",
            "\"${localProperties.getProperty("AI_API_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "AI_BASE_URL",
            "\"${localProperties.getProperty("AI_BASE_URL", "https://api.groq.com/openai/v1")}\""
        )
        buildConfigField(
            "String",
            "AI_MODEL",
            "\"${localProperties.getProperty("AI_MODEL", "llama-3.3-70b-versatile")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.leanback)
    implementation(libs.glide)
    implementation(libs.material)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")
}

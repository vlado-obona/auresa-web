import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "sk.rallysupport.otackomer"
    compileSdk = 35

    defaultConfig {
        applicationId = "sk.rallysupport.otackomer"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // Aplikácia je len po slovensky – ostatné jazyky netreba baliť.
        resourceConfigurations += listOf("sk")
    }

    // Pevný "debug" kľúč commitnutý v repe (heslo "android"): nechráni nič,
    // len zaručí, že APK z CI aj z Android Studia majú rovnaký podpis a
    // aktualizácia sa dá nainštalovať cez staršiu verziu bez odinštalovania.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/otackomer-debug.jks")
            storePassword = "android"
            keyAlias = "otackomer"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main").kotlin.srcDirs("src/main/kotlin")
        getByName("test").kotlin.srcDirs("src/test/kotlin")
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}

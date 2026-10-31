plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.auratv.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.auratv.launcher"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "BUILD_LABEL", "\"\"")
    }

    signingConfigs {
        create("release") {
            // Release credentials are supplied by GitHub Actions Secrets.
            // Never commit the keystore or passwords to this repository.
            val releaseStoreFile = providers.environmentVariable("RELEASE_STORE_FILE").orNull
            val releaseStorePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
            val releaseKeyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
            val releaseKeyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull

            if (!releaseStoreFile.isNullOrBlank()) {
                storeFile = file(releaseStoreFile)
            }
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // ---- v1.1.0 development-only additions ----------------------------------------------
    // The production signing config and the release build type above are NOT touched.
    // The dev key below is public on purpose, is never used for releases, and must never
    // be referenced from the release build type.
    signingConfigs {
        create("dev") {
            storeFile = file("dev/aura-dev.keystore")
            storePassword = "auradev"
            keyAlias = "auradev"
            keyPassword = "auradev"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            signingConfig = signingConfigs.getByName("dev")
            buildConfigField("String", "BUILD_LABEL", "\"v1.1.0 dev preview\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
}

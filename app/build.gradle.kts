import java.util.UUID

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.bugsnag.android)
}

android {
    namespace = "dev.zwander.installwithoptions"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.zwander.installwithoptions"
        minSdk = 24
        targetSdk = 35
        versionCode = 20
        versionName = "0.7.3"

        vectorDrawables {
            useSupportLibrary = true
        }

        extensions.getByType(BasePluginExtension::class.java).archivesName.set("InstallWithOptions_${versionName}")
        manifestPlaceholders["build_uuid"] = UUID.nameUUIDFromBytes("InstallWithOptions_${versionCode}".toByteArray()).toString()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
        compose = true
        buildConfig = true
        aidl = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlin.reflect)
    implementation(libs.androidx.documentfile)
    implementation(libs.hiddenapibypass)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.moko.mvvm)
    implementation(libs.androidx.preference)
    implementation(libs.gson)
    implementation(libs.patreonSupportersRetrieval)
    implementation(libs.bugsnag.android) {
        exclude("com.bugsnag", "bugsnag-plugin-android-anr")
        exclude("com.bugsnag", "bugsnag-plugin-android-ndk")
    }
    implementation(libs.bugsnag.android.performance)
    implementation(libs.material.components)
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.zip4j)
}

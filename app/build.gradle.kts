plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun env(name: String): String? = providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }

android {
    namespace = "de.mef.lebensmittel"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.mef.lebensmittel"
        minSdk = 26
        targetSdk = 35
        // CI sets these from the git tag ("1.2.3" -> 10203); must grow so phones accept the update
        versionCode = env("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = env("VERSION_NAME") ?: "1.0"
    }

    // The app on the phone (release 1.0) is signed with the Android debug key of the dev PC.
    // Updates keep the user's data only when signed with that same key, so CI gets it as a secret.
    signingConfigs {
        create("release") {
            val ks = env("SIGNING_KEYSTORE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = env("SIGNING_STORE_PASSWORD")
                keyAlias = env("SIGNING_KEY_ALIAS")
                keyPassword = env("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // locally (no SIGNING_KEYSTORE) the debug key is used, which is the same key on the dev PC
            signingConfig = if (env("SIGNING_KEYSTORE") != null) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // product photos
    implementation("io.coil-kt:coil-compose:2.7.0")
    // barcode scanner from Google Play services (no camera permission needed)
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

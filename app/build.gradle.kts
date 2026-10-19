plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The only place a release version is written down (see "Versioning" in the README).
// MAJOR.MINOR.PATCH; versionCode is derived from it so the two can never disagree.
val appVersionName = "1.4.0"

fun versionCodeOf(name: String): Int {
    val (major, minor, patch) = name.split('.').map { it.toInt() }
    return major * 10_000 + minor * 100 + patch // 1.3.0 -> 10300
}

android {
    namespace = "com.featherw8.player"
    compileSdk = 37
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.featherw8.player"
        minSdk = 28
        targetSdk = 36
        versionCode = versionCodeOf(appVersionName)
        versionName = appVersionName
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++20"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // The project key comes from the environment (CI restores it from a secret), so it never lives in the
    // repository. Without it - local builds, pull requests from forks - the standard Android debug key is
    // used, which only means such an APK cannot be installed over one signed with the project key.
    val keystorePath = System.getenv("FEATHERW8_KEYSTORE")
    if (keystorePath != null) {
        signingConfigs {
            create("project") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("FEATHERW8_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("FEATHERW8_KEY_ALIAS")
                keyPassword = System.getenv("FEATHERW8_KEYSTORE_PASSWORD")
            }
        }
    }
    val appSigning = signingConfigs.findByName("project") ?: signingConfigs.getByName("debug")

    buildTypes {
        release {
            // R8 and resource shrinking: a Compose app is much faster (and the APK several times smaller)
            // than the unoptimised debug build, which also drags in the whole extended icon set.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = appSigning
        }
        debug {
            signingConfig = appSigning
        }
    }

    lint {
        // Lint is not part of this project's checks; do not let its vital pass fail a release build.
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-extractor:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
}

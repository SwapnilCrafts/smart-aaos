/*
 * Builds the Car System UI overlay APK.
 *
 * Deliberately minimal: an RRO has no code, so there is no Kotlin plugin, no
 * dependencies and no source directory. `hasCode="false"` in the manifest
 * tells PackageManager not to expect a dex file.
 *
 * The resulting APK is debug-signed, which is fine for a runtime
 * (isStatic=false) overlay pushed to /vendor/overlay on a userdebug build. A
 * production OEM overlay ships inside the system image and is signed with the
 * platform key.
 */
plugins {
    id("com.android.application")
}

android {
    namespace = "com.swapnil.smartaaos.systemui.overlay"
    compileSdk = 35

    defaultConfig {
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    // Nothing to shrink or optimise - the APK is a resource table.
    androidResources {
        // Keep resource names intact so the overlay can match by name.
        additionalParameters += listOf("--no-version-vectors")
    }
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.filipnone.dashglanceclock"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.filipnone.dashglanceclock"
        // java.time needs API 26. Going lower means turning on core library desugaring.
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "0.3.0-beta"
    }

    // Two apps from one module. Android Auto drops a package's media service when the same
    // package has a Car App Library service, so the music card and the full-screen clock only
    // both show up as separate packages. The card keeps the old single app's ID, so updating
    // 0.2 installs it and keeps the music controls setting.
    flavorDimensions += "app"
    productFlavors {
        create("fullscreen") {
            dimension = "app"
            applicationIdSuffix = ".fullscreen"
        }
        create("card") {
            dimension = "app"
        }
    }

    // The keystore and passwords live outside the repo, in ~/.gradle/gradle.properties.
    // Without them, release builds come out unsigned.
    val releaseKeystore = providers.gradleProperty("dashglance.storeFile").orNull
    val releaseSigning = releaseKeystore?.let {
        signingConfigs.create("release") {
            storeFile = file(it)
            storePassword = providers.gradleProperty("dashglance.storePassword").get()
            keyAlias = providers.gradleProperty("dashglance.keyAlias").get()
            keyPassword = providers.gradleProperty("dashglance.keyPassword").get()
        }
    }

    buildTypes {
        release {
            signingConfig = releaseSigning
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.annotation)
    "fullscreenImplementation"(libs.androidx.car.app)
    "fullscreenImplementation"(libs.material)
    "cardImplementation"(libs.androidx.media3.session)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    "testFullscreenImplementation"(libs.androidx.car.app.testing)
}

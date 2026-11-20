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
        versionCode = 2
        versionName = "0.2.0-beta"
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
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.annotation)
    implementation(libs.material)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.car.app.testing)
}

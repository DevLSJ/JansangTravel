import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.jansangtravel.krwqpz"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    
    // Read MAPS_API_KEY from local-only sources. Do not fall back to .env.example,
    // because placeholder keys make Google Maps render a white map with only the logo.
    val envFile = rootProject.file(".env")
    val envProperties = Properties()
    if (envFile.exists()) {
        val stream = envFile.inputStream()
        try {
            envProperties.load(stream)
        } finally {
            stream.close()
        }
    }

    val localPropertiesFile = rootProject.file("local.properties")
    val localProperties = Properties()
    if (localPropertiesFile.exists()) {
        val stream = localPropertiesFile.inputStream()
        try {
            localProperties.load(stream)
        } finally {
            stream.close()
        }
    }

    fun isPlaceholderMapsKey(value: String?): Boolean {
        val key = value?.trim().orEmpty()
        if (key.isEmpty()) return true
        val upper = key.uppercase()
        return upper == "YOUR_GOOGLE_MAPS_API_KEY" ||
            upper == "REPLACE_WITH_YOUR_GOOGLE_MAPS_API_KEY" ||
            upper == "PLACEHOLDER_KEY" ||
            upper.contains("PLACEHOLDER")
    }

    val mapsKey = listOf(
        envProperties.getProperty("MAPS_API_KEY"),
        localProperties.getProperty("MAPS_API_KEY"),
        System.getenv("MAPS_API_KEY")
    ).firstOrNull { !isPlaceholderMapsKey(it) }?.trim().orEmpty()

    manifestPlaceholders["MAPS_API_KEY"] = mapsKey
    resValue("string", "google_maps_key", mapsKey)
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    viewBinding = true
    buildConfig = true
    resValues = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.fragment:fragment-ktx:1.8.1")
  implementation("androidx.recyclerview:recyclerview:1.3.2")
  implementation("androidx.exifinterface:exifinterface:1.3.7")
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation("com.google.android.gms:play-services-maps:19.0.0")
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

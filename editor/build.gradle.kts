plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "dev.mangolabs.quilleditor"
  compileSdk = 36

  defaultConfig {
    minSdk = 34
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildFeatures {
    compose = true
    buildConfig = false
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin {
    jvmToolchain(17)
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
}

dependencies {
  // Compose surface — used by QuillEditor/QuillToolbar composables (Phase 3+).
  val composeBom = platform(libs.compose.bom)
  api(composeBom)
  api(libs.compose.ui)
  api(libs.compose.ui.graphics)
  api(libs.compose.material3)
  api(libs.compose.material.icons.ext)

  // Serialization for Delta / ActiveFormat (Phase 2+).
  api(libs.kotlinx.serialization.json)

  // Test
  testImplementation(libs.junit)

  androidTestImplementation(composeBom)
  androidTestImplementation(libs.androidx.test.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.rules)
  androidTestImplementation(libs.compose.ui.test.junit4)
  debugImplementation(libs.compose.ui.test.manifest)
  debugImplementation(libs.compose.ui.tooling)
}

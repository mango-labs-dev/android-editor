plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.dokka)
  `maven-publish`
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

  publishing {
    singleVariant("release") {
      withSourcesJar()
    }
  }
}

// Jar of Dokka HTML output, attached to the release publication as the
// `javadoc` classifier so Maven consumers see API docs alongside sources.
val dokkaJavadocJar by tasks.registering(Jar::class) {
  dependsOn(tasks.named("dokkaGenerate"))
  from(layout.buildDirectory.dir("dokka/html"))
  archiveClassifier.set("javadoc")
}

publishing {
  publications {
    register<MavenPublication>("release") {
      groupId = "dev.mangolabs"
      artifactId = "quill-compose-editor"
      version = "0.1.0-SNAPSHOT"

      afterEvaluate {
        from(components["release"])
        artifact(dokkaJavadocJar)
      }

      pom {
        name.set("Quill Compose Editor")
        description.set(
          "Jetpack Compose rich text editor backed by Quill.js — a clean Kotlin API over a Quill-powered WebView."
        )
        url.set("https://github.com/mango-labs-dev/android-editor")
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        developers {
          developer {
            id.set("mangolabs")
            name.set("Mango Labs")
          }
        }
        scm {
          url.set("https://github.com/mango-labs-dev/android-editor")
          connection.set("scm:git:git://github.com/mango-labs-dev/android-editor.git")
          developerConnection.set("scm:git:ssh://git@github.com/mango-labs-dev/android-editor.git")
        }
      }
    }
  }

  repositories {
    mavenLocal()
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
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.compose.ui.test.junit4)
  debugImplementation(libs.compose.ui.test.manifest)
  debugImplementation(libs.compose.ui.tooling)
}

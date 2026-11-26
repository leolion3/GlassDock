plugins {
    alias(libs.plugins.android.application)
}

    android {
    namespace = "com.glassdock.launcher"
        compileSdk {
            version = release(37)
        }

        defaultConfig {
      applicationId = "com.glassdock.launcher"
    minSdk = 25
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"

      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

        buildTypes {
            release {
                isMinifyEnabled = false
                isShrinkResources = false
                signingConfig = signingConfigs.getByName("debug")
                optimization {
                    enable = false
                }
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }

  dependencies {
      implementation(libs.androidx.activity)
      testImplementation(libs.junit)
      androidTestImplementation(libs.androidx.espresso.core)
      androidTestImplementation(libs.androidx.junit)
  }
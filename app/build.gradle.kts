plugins {
    alias(libs.plugins.carenote.android.application)
    alias(libs.plugins.carenote.android.compose)
    alias(libs.plugins.carenote.android.hilt)
}

android {
    namespace = "com.hossam.carenote"

    defaultConfig {
        applicationId = "com.hossam.carenote"
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    // For Hilt only: the app wires these implementations, features never see them.
    implementation(projects.core.data)
    implementation(projects.core.speech)
    implementation(projects.feature.residents)
    implementation(projects.feature.resident)
    implementation(projects.feature.record)

    implementation(libs.timber)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

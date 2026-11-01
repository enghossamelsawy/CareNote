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

    // Only the app has flavors; library modules stay flavor-free and get what differs through Hilt.
    flavorDimensions += "environment"
    productFlavors {
        // Talks to the in-memory mock server, for demos and development without a backend.
        create("mock") {
            dimension = "environment"
            applicationIdSuffix = ".mock"
            versionNameSuffix = "-mock"
        }
        create("prod") {
            dimension = "environment"
        }
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
    implementation(projects.core.network)
    implementation(projects.core.speech)
    // The mock server exists only in mock builds; prod contains none of its code.
    "mockImplementation"(projects.core.networkMock)
    implementation(projects.feature.residents)
    implementation(projects.feature.resident)
    implementation(projects.feature.record)

    implementation(libs.timber)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
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

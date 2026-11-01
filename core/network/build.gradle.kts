plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hossam.carenote.core.network"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // TODO(4.3): provided per flavor by the app instead.
        buildConfigField("String", "BASE_URL", "\"https://api.carenote.example/\"")
    }
}

dependencies {
    // RemoteDataSource speaks in domain models.
    api(projects.core.model)
    implementation(projects.core.common)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}

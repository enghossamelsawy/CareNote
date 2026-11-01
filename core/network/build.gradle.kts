plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hossam.carenote.core.network"

    // For BuildConfig.DEBUG, which turns HTTP logging on. The base URL comes from the app's flavor.
    buildFeatures {
        buildConfig = true
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

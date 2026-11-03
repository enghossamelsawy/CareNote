plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hossam.carenote.core.network"
}

dependencies {
    // RemoteDataSource speaks in domain models.
    api(projects.core.model)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}

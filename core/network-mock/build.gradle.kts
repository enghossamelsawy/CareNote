plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.hilt)
}

android {
    namespace = "com.hossam.carenote.core.network.mock"
}

// Only the mock flavor of the app depends on this module, so prod builds contain none of it.
dependencies {
    implementation(projects.core.network)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
}

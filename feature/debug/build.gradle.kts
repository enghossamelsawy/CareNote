plugins {
    alias(libs.plugins.carenote.android.feature)
}

android {
    namespace = "com.hossam.carenote.feature.debug"
}

// Only the mock flavor of the app depends on this module; it drives the mock server, so prod has neither.
dependencies {
    implementation(projects.core.network)
    implementation(projects.core.networkMock)
    implementation(projects.core.speech)

    testImplementation(projects.core.testing)
}

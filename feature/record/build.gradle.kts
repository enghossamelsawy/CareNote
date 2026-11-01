plugins {
    alias(libs.plugins.carenote.android.feature)
    alias(libs.plugins.carenote.android.roborazzi)
}

android {
    namespace = "com.hossam.carenote.feature.record"
}

dependencies {
    implementation(projects.core.domain)
    // The microphone permission request.
    implementation(libs.androidx.activity.compose)

    testImplementation(projects.core.testing)
}

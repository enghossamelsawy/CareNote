plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.hilt)
}

android {
    namespace = "com.hossam.carenote.core.speech"
}

dependencies {
    // Implements the domain's SpeechTranscriber; Android's recognizer stays an implementation detail.
    implementation(projects.core.domain)
    implementation(projects.core.common)
}

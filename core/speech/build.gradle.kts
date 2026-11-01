plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.hilt)
}

android {
    namespace = "com.hossam.carenote.core.speech"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Robolectric reaches into FileDescriptor internals, which newer JDKs (here 25) no longer expose by default.
tasks.withType<Test>().configureEach {
    jvmArgs(
        "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
    )
}

dependencies {
    // Implements the domain's SpeechTranscriber; Android's recognizer stays an implementation detail.
    implementation(projects.core.domain)
    implementation(projects.core.common)
    implementation(libs.mlkit.genai.speech.recognition)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

plugins {
    alias(libs.plugins.carenote.android.feature)
}

android {
    namespace = "com.hossam.carenote.feature.resident"
}

dependencies {
    implementation(projects.core.domain)

    testImplementation(projects.core.testing)
}

plugins {
    alias(libs.plugins.carenote.android.feature)
    alias(libs.plugins.carenote.android.roborazzi)
}

android {
    namespace = "com.hossam.carenote.feature.residents"
}

dependencies {
    implementation(projects.core.domain)

    testImplementation(projects.core.testing)
}

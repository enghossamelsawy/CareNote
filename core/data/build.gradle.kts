plugins {
    alias(libs.plugins.carenote.android.library)
}

android {
    namespace = "com.hossam.carenote.core.data"
}

dependencies {
    // Implements the domain's interfaces; Room stays an implementation detail.
    api(projects.core.domain)
    implementation(projects.core.database)
}

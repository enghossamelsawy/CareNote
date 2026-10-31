plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.room)
}

android {
    namespace = "com.hossam.carenote.core.database"
}

dependencies {
    implementation(projects.core.model)
}

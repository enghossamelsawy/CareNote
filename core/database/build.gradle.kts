plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.robolectric)
    alias(libs.plugins.carenote.android.room)
    alias(libs.plugins.carenote.android.hilt)
}

android {
    namespace = "com.hossam.carenote.core.database"
}

dependencies {
    implementation(projects.core.model)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

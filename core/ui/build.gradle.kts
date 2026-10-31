plugins {
    alias(libs.plugins.carenote.android.library)
}

android {
    namespace = "com.hossam.carenote.core.ui"
}

dependencies {
    // Screens extend MviViewModel and expose its StateFlow, so both types are part of this module's API.
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.kotlinx.coroutines.core)
}

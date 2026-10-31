plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.compose)
}

android {
    namespace = "com.hossam.carenote.core.designsystem"
}

dependencies {
    // Features use MaterialTheme, Text and the other Material components directly.
    api(libs.androidx.compose.material3)
}

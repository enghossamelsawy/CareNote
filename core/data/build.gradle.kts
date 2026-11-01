plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.robolectric)
    alias(libs.plugins.carenote.android.hilt)
}

android {
    namespace = "com.hossam.carenote.core.data"
}

dependencies {
    // Implements the domain's interfaces; Room stays an implementation detail.
    api(projects.core.domain)
    implementation(projects.core.database)
    implementation(projects.core.network)
    implementation(projects.core.common)

    // Background sync: WorkManager, with workers created by Hilt.
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    "ksp"(libs.androidx.hilt.compiler)

    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(projects.core.testing)
}

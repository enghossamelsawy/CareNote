plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.hilt)
}

android {
    namespace = "com.hossam.carenote.core.data"

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
    // Implements the domain's interfaces; Room stays an implementation detail.
    api(projects.core.domain)
    implementation(projects.core.database)

    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}

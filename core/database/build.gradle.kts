plugins {
    alias(libs.plugins.carenote.android.library)
    alias(libs.plugins.carenote.android.room)
}

android {
    namespace = "com.hossam.carenote.core.database"

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
    implementation(projects.core.model)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

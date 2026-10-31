plugins {
    alias(libs.plugins.carenote.jvm.library)
}

// Shared test helpers; other modules add it as testImplementation only.
dependencies {
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    // Fakes of the domain interfaces, shared by use case and ViewModel tests.
    api(projects.core.domain)
}

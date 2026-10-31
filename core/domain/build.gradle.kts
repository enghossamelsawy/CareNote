plugins {
    alias(libs.plugins.carenote.jvm.library)
}

dependencies {
    // Domain types and Flow appear in the public interfaces.
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
}

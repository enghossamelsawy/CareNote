import com.hossam.carenote.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A Compose feature module with Hilt ViewModels and Navigation 3 (serializable NavKey routes). */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("carenote.android.library")
            pluginManager.apply("carenote.android.compose")
            pluginManager.apply("carenote.android.hilt")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-navigation3").get())
                add("implementation", libs.findLibrary("androidx-navigation3-runtime").get())
                add("implementation", libs.findLibrary("androidx-navigation3-ui").get())

                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
                add("testImplementation", libs.findLibrary("turbine").get())
            }
        }
    }
}

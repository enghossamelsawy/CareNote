import com.hossam.carenote.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Screenshot tests of Compose screens, rendered on the JVM by Robolectric. Images are only written or
 * compared by the Roborazzi tasks (`recordRoborazziDebug`, `verifyRoborazziDebug`); a plain `test` skips them.
 */
class AndroidRoborazziConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("carenote.android.robolectric")
            pluginManager.apply("io.github.takahirom.roborazzi")

            dependencies {
                add("testImplementation", libs.findLibrary("roborazzi").get())
                add("testImplementation", libs.findLibrary("roborazzi-compose").get())
            }
        }
    }
}

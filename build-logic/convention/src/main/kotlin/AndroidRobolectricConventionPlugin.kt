import com.android.build.api.dsl.LibraryExtension
import com.hossam.carenote.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * JVM unit tests that run Android code through Robolectric. The module still needs
 * `src/test/resources/robolectric.properties` to pin the SDK Robolectric supports.
 */
class AndroidRobolectricConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<LibraryExtension> {
                testOptions.unitTests.isIncludeAndroidResources = true
            }

            // Robolectric reaches into FileDescriptor internals, which newer JDKs (here 25) no longer expose.
            tasks.withType<Test>().configureEach {
                jvmArgs(
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                )
            }

            dependencies {
                add("testImplementation", libs.findLibrary("robolectric").get())
                add("testImplementation", libs.findLibrary("androidx-test-core").get())
            }
        }
    }
}

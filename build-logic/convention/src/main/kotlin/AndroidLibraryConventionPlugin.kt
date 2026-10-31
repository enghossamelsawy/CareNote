import com.android.build.api.dsl.LibraryExtension
import com.hossam.carenote.buildlogic.CareNoteConfig
import com.hossam.carenote.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = CareNoteConfig.COMPILE_SDK
                defaultConfig.apply {
                    minSdk = CareNoteConfig.MIN_SDK
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                // With AGP's built-in Kotlin, the Kotlin JVM target follows these.
                compileOptions.apply {
                    sourceCompatibility = CareNoteConfig.JAVA_VERSION
                    targetCompatibility = CareNoteConfig.JAVA_VERSION
                }
            }

            dependencies {
                add("testImplementation", libs.findLibrary("junit").get())
            }
        }
    }
}

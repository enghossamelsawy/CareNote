import com.android.build.api.dsl.ApplicationExtension
import com.hossam.carenote.buildlogic.CareNoteConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = CareNoteConfig.COMPILE_SDK
                defaultConfig.apply {
                    minSdk = CareNoteConfig.MIN_SDK
                    targetSdk = CareNoteConfig.TARGET_SDK
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                // With AGP's built-in Kotlin, the Kotlin JVM target follows these.
                compileOptions.apply {
                    sourceCompatibility = CareNoteConfig.JAVA_VERSION
                    targetCompatibility = CareNoteConfig.JAVA_VERSION
                }
            }
        }
    }
}

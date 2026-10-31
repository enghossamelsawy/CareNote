import com.hossam.carenote.buildlogic.CareNoteConfig
import com.hossam.carenote.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Pure Kotlin module without any Android dependency, e.g. model and domain. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = CareNoteConfig.JAVA_VERSION
                targetCompatibility = CareNoteConfig.JAVA_VERSION
            }
            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions.jvmTarget.set(JvmTarget.fromTarget(CareNoteConfig.JAVA_VERSION.toString()))
            }

            dependencies {
                add("testImplementation", libs.findLibrary("junit").get())
            }
        }
    }
}

package com.hossam.carenote.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Adds ktlintCheck / ktlintFormat with the Compose rules. Rules come from the root .editorconfig. */
internal fun Project.configureKtlint() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")

    dependencies {
        add("ktlintRuleset", libs.findLibrary("compose-rules-ktlint").get())
    }
}

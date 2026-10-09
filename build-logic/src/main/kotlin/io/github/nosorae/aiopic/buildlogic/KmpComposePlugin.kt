package io.github.nosorae.aiopic.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** ui/design, ui/navigation, 앱 셸 용. KMP 라이브러리에 Compose Multiplatform 을 얹는다. */
class KmpComposePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply<KmpLibraryPlugin>()
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
                androidResources.enable = true
            }
            sourceSets.commonMain.dependencies {
                implementation(library("compose-runtime"))
                implementation(library("compose-foundation"))
                implementation(library("compose-material3"))
                implementation(library("compose-ui"))
                implementation(library("compose-components-resources"))
                implementation(library("compose-uiToolingPreview"))
            }
        }

        extensions.configure<ComposeExtension> {
            extensions.configure<ResourcesExtension> {
                packageOfResClass = namespaceOf(path)
            }
        }
        configureRoborazzi()
    }
}

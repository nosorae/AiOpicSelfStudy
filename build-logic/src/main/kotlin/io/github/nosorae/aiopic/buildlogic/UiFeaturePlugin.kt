package io.github.nosorae.aiopic.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * ui/feature-* 용. 화면마다 똑같이 필요한 디자인, 내비게이션, ViewModel 의존을 한 번에 건다.
 * 시험 팩은 앱이 조립해서 넘기므로 여기서 걸지 않는다.
 */
class UiFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply<KmpComposePlugin>()

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                implementation(project(":ui:design"))
                implementation(project(":ui:navigation"))
                implementation(library("androidx-lifecycle-viewmodelCompose"))
                implementation(library("androidx-lifecycle-runtimeCompose"))
            }
            sourceSets.commonTest.dependencies {
                implementation(library("kotlinx-coroutines-test"))
            }
        }
    }
}

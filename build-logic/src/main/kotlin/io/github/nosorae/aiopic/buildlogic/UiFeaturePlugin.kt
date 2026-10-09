package io.github.nosorae.aiopic.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * ui/feature-* 용. 화면마다 똑같이 필요한 디자인, 내비게이션, ViewModel 의존을 한 번에 건다.
 * 시험 팩은 앱이 조립해서 넘기므로 여기서 걸지 않는다.
 * ViewModel 이 화면 인자를 assisted 주입으로 받으므로(ADR 0007) Metro 컴파일러 플러그인도 건다.
 */
class UiFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply<KmpComposePlugin>()
        pluginManager.apply("dev.zacsweers.metro")

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

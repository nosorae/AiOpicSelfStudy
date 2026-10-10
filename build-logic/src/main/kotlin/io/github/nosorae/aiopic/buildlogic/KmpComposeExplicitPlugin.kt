package io.github.nosorae.aiopic.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** 화면을 가진 libs 용(`libs/debug`). Compose 를 얹고, 다른 앱이 가져다 쓰는 모듈이라 explicit API 를 켠다. */
class KmpComposeExplicitPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply<KmpComposePlugin>()
        extensions.configure<KotlinMultiplatformExtension> {
            explicitApi()
        }
    }
}

package io.github.nosorae.aiopic.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** libs 용. 여러 층이 가져다 쓰는 모듈이라 공개 범위를 선언하지 않은 API 가 새어 나가지 않게 explicit API 를 켠다. */
class KmpLibraryExplicitPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply<KmpLibraryPlugin>()
        extensions.configure<KotlinMultiplatformExtension> {
            explicitApi()
        }
    }
}

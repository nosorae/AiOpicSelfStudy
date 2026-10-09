package io.github.nosorae.aiopic.buildlogic

import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Compose 모듈의 Android 호스트 테스트에서 화면을 PNG 로 찍어 기준 이미지와 비교한다.
 * 기준 이미지는 `src/androidHostTest/screenshots/` 에 커밋하고, `check` 가 비교(verify)를 돈다.
 * 기준 이미지를 새로 찍을 때는 `./gradlew recordRoborazziAndroidHostTest` 를 쓴다.
 */
internal fun Project.configureRoborazzi() {
    pluginManager.apply("io.github.takahirom.roborazzi")

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.getByName("androidHostTest").dependencies {
            implementation(library("roborazzi-compose"))
            implementation(library("robolectric"))
            implementation(library("androidx-testExt-junit"))
            implementation(library("junit"))
        }
    }
    extensions.configure<RoborazziExtension> {
        outputDir.set(layout.projectDirectory.dir("src/androidHostTest/screenshots"))
    }
    tasks.named("check") {
        dependsOn("verifyRoborazziAndroidHostTest")
    }
}

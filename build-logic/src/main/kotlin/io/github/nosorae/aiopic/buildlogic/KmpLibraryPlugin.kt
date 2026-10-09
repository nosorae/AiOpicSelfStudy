package io.github.nosorae.aiopic.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** engine, packs 용. Android 와 iOS(기기, Apple Silicon 시뮬레이터)를 대상으로 하는 KMP 라이브러리. */
class KmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        // KMP Android 라이브러리는 이 플러그인을 걸어야 lint 태스크가 생기고 check 에 들어간다.
        pluginManager.apply("com.android.lint")

        extensions.configure<KotlinMultiplatformExtension> {
            iosArm64()
            iosSimulatorArm64()

            extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
                namespace = namespaceOf(path)
                compileSdk = requiredIntVersion("android-compileSdk")
                minSdk = requiredIntVersion("android-minSdk")
                // Robolectric 스크린샷 테스트가 Android 리소스를 읽을 수 있게 넣는다. 리소스 없는 모듈에는 영향이 없다.
                withHostTest { isIncludeAndroidResources = true }
                // 오류는 빌드를 멈추고 경고는 보고서에만 남긴다.
                lint {
                    abortOnError = true
                    warningsAsErrors = false
                }
            }

            sourceSets.commonTest.dependencies {
                implementation(library("kotlin-test"))
            }
        }
        val jvm = JvmTarget.fromTarget(requiredVersion("jvm"))
        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions.jvmTarget.set(jvm)
        }
    }
}

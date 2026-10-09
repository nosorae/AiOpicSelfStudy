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

        extensions.configure<KotlinMultiplatformExtension> {
            iosArm64()
            iosSimulatorArm64()

            extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
                namespace = namespaceOf(path)
                compileSdk = requiredIntVersion("android-compileSdk")
                minSdk = requiredIntVersion("android-minSdk")
                withHostTest {}
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

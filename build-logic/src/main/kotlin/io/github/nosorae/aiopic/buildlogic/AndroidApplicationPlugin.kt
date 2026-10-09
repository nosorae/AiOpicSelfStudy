package io.github.nosorae.aiopic.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Android 앱 모듈 용. applicationId 와 버전은 앱 모듈이 정한다. */
class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val jvm = requiredVersion("jvm")
        extensions.configure<ApplicationExtension> {
            namespace = namespaceOf(path)
            compileSdk = requiredIntVersion("android-compileSdk")
            defaultConfig {
                minSdk = requiredIntVersion("android-minSdk")
                targetSdk = requiredIntVersion("android-targetSdk")
            }
            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(jvm)
                targetCompatibility = JavaVersion.toVersion(jvm)
            }
            buildFeatures {
                compose = true
            }
            // 오류는 빌드를 멈추고 경고는 보고서에만 남긴다.
            lint {
                abortOnError = true
                warningsAsErrors = false
            }
        }
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget(jvm))
        }
    }
}

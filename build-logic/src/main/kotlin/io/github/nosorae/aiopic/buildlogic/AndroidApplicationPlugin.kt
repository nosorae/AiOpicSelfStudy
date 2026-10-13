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
            // 업로드 키는 저장소 밖에 두고 경로만 받는다. 없으면 서명 없이 빌드하고 이유를 찍는다(README 「배포용 빌드」).
            readReleaseSigning(localProperty(RELEASE_SIGNING_KEY) ?: System.getenv(RELEASE_SIGNING_KEY))
                .onSuccess { signing ->
                    val release = signingConfigs.create("release") {
                        storeFile = signing.storeFile
                        storePassword = signing.storePassword
                        keyAlias = signing.keyAlias
                        keyPassword = signing.keyPassword
                    }
                    buildTypes.getByName("release").signingConfig = release
                }
                .onFailure { logger.lifecycle("릴리스 빌드를 서명 없이 만든다: ${it.message}") }
        }
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget(jvm))
        }
    }
}

private fun Project.localProperty(key: String): String? {
    val file = rootProject.file("local.properties")
    if (!file.isFile) return null
    return java.util.Properties().apply { file.inputStream().use(::load) }.getProperty(key)
}

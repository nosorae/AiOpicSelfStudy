package io.github.nosorae.aiopic.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** 카탈로그에 값이 없으면 빈 값으로 빌드되지 않게 설정 단계에서 바로 실패한다. */
internal fun Project.requiredVersion(name: String): String = libs.findVersion(name).orElseThrow {
    IllegalStateException("gradle/libs.versions.toml 의 [versions] 에 '$name' 이 없다.")
}.requiredVersion

internal fun Project.requiredIntVersion(name: String): Int = requiredVersion(name).toIntOrNull()
    ?: error("gradle/libs.versions.toml 의 '$name' 은 정수여야 한다: '${requiredVersion(name)}'")

internal fun Project.library(alias: String): Provider<MinimalExternalModuleDependency> = libs.findLibrary(alias).orElseThrow {
    IllegalStateException("gradle/libs.versions.toml 의 [libraries] 에 '$alias' 가 없다.")
}

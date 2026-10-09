package io.github.nosorae.aiopic.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/** 루트 프로젝트에 걸어 `checkLayers` 를 `check` 에 연결한다. */
class LayerCheckPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        check(this == rootProject) { "aiopic.layer-check 는 루트 프로젝트에만 건다." }
        pluginManager.apply("base")

        val checkLayers = tasks.register<CheckLayersTask>("checkLayers") {
            group = "verification"
            description = "모듈 사이 의존이 층 규칙을 지키는지 검사한다."
        }
        // 실행 단계에서 Project 를 읽으면 configuration cache 와 맞지 않아 설정 단계에서 의존 목록을 모아 입력으로 넘긴다.
        gradle.projectsEvaluated {
            val dependencies = subprojects.associate { module ->
                module.path to module.configurations
                    .flatMap { it.dependencies.withType<ProjectDependency>().map(ProjectDependency::getPath) }
                    .filter { it != module.path }
                    .distinct()
                    .sorted()
            }
            checkLayers.configure { this.dependencies.set(dependencies) }
        }
        tasks.named("check") { dependsOn(checkLayers) }
    }
}

abstract class CheckLayersTask : DefaultTask() {
    @get:Input
    abstract val dependencies: MapProperty<String, List<String>>

    @TaskAction
    fun checkLayers() {
        val violations = layerViolations(dependencies.get())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "층 의존 규칙을 어긴 의존 ${violations.size}건\n" + violations.joinToString("\n") { "- $it" },
            )
        }
    }
}

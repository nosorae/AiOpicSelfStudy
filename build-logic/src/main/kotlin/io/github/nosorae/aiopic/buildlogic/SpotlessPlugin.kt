package io.github.nosorae.aiopic.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * 루트 프로젝트에 한 번 걸어 저장소의 모든 .kt, .kts 를 ktlint 로 맞춘다. `check` 가 `spotlessCheck` 를 돈다.
 * 모듈마다 걸지 않아 새 모듈을 만들어도 설정을 잊을 일이 없다.
 */
class SpotlessPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        check(this == rootProject) { "aiopic.spotless 는 루트 프로젝트에만 건다." }
        pluginManager.apply("com.diffplug.spotless")

        val ktlintVersion = requiredVersion("ktlint")
        // Compose 는 화면 함수를 대문자로 시작하는 규칙을 따르므로 @Composable 함수만 함수 이름 규칙에서 뺀다.
        // 빈 파일 규칙은 끈다. 아직 코드가 없는 모듈은 패키지 선언만 있는 Package.kt 로 소스 폴더를 잡아 둔다.
        val editorConfig = mapOf(
            "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
            "ktlint_standard_no-empty-file" to "disabled",
        )
        // Compose 규칙 묶음은 .kt 에만 건다. 기본값으로 켜진 규칙이 화면 함수 이름(대문자 시작), 미리보기 비공개,
        // ViewModel 을 자식 컴포저블에 넘기지 않기를 잡는다. 화면은 Material 3 만 쓰므로 Material 2 사용을 막는 규칙을 켠다.
        val composeRules = library("composeRules-ktlint").get().let { "${it.group}:${it.name}:${it.version}" }
        // CompositionLocal 은 숨은 의존이라 막고, 거의 모든 화면이 쓰는 진동(ui/design 의 LocalHaptics)만 허용한다.
        val composeEditorConfig = editorConfig + ("compose_disallow_material2" to "true") + ("compose_allowed_composition_locals" to "LocalHaptics")
        val excluded = listOf("**/build/**", "**/.gradle/**", "**/.kotlin/**")

        extensions.configure<SpotlessExtension> {
            kotlin {
                target("**/*.kt")
                targetExclude(excluded)
                ktlint(ktlintVersion).editorConfigOverride(composeEditorConfig).customRuleSets(listOf(composeRules))
            }
            kotlinGradle {
                target("**/*.kts")
                targetExclude(excluded)
                ktlint(ktlintVersion).editorConfigOverride(editorConfig)
            }
        }
    }
}

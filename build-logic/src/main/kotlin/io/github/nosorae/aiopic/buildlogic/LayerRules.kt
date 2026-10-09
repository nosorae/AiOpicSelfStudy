package io.github.nosorae.aiopic.buildlogic

const val ROOT_PACKAGE = "io.github.nosorae.aiopic"

/**
 * 층(모듈 경로의 첫 마디)마다 의존해도 되는 층.
 * 아래 층이 위 층을 모르게 해서 엔진과 라이브러리를 화면 없이 테스트하고,
 * 시험 팩을 바꿔 끼워도 화면 코드가 영향을 받지 않게 한다.
 */
private val allowedLayers: Map<String, Set<String>> = mapOf(
    "libs" to emptySet(),
    "engine" to setOf("engine", "libs"),
    "packs" to setOf("engine", "libs"),
    "ui" to setOf("ui", "engine", "libs"),
    "apps" to setOf("apps", "ui", "packs", "engine", "libs"),
    "tools" to setOf("engine", "packs", "libs"),
)

private fun layerOf(path: String): String = path.removePrefix(":").substringBefore(":")

/** 모듈 경로별 프로젝트 의존 목록을 받아, 층 규칙을 어긴 의존마다 사람이 읽을 메시지 하나를 돌려준다. */
fun layerViolations(dependencies: Map<String, List<String>>): List<String> =
    dependencies.flatMap { (from, targets) ->
        val layer = layerOf(from)
        val allowed = allowedLayers[layer]
            ?: return@flatMap listOf(
                "'$from' 의 층 '$layer' 을 모른다. 고칠 방법: 모듈을 ${allowedLayers.keys} 중 한 층 아래로 옮기거나 층 규칙에 '$layer' 를 더한다.",
            )
        val rule = if (allowed.isEmpty()) "프로젝트 모듈을 의존할 수 없다" else "${allowed.joinToString()} 층만 의존할 수 있다"
        targets.filter { layerOf(it) !in allowed }.map { to ->
            "'$from' 이 '$to' 를 의존한다. $layer 층은 $rule. " +
                "고칠 방법: 의존을 빼거나, 필요한 코드를 '$layer' 가 의존할 수 있는 층으로 내린다."
        }
    }

/** 모듈 경로로 Android namespace 와 Compose Resources 패키지를 만든다. `-` 를 빼고 소문자로 바꾼다. `:apps:opic:androidApp` 은 `…apps.opic.androidapp`. */
fun namespaceOf(path: String): String =
    ROOT_PACKAGE + path.replace("-", "").replace(":", ".").lowercase()

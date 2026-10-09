package io.github.nosorae.aiopic.packs.opicen

/**
 * 프롬프트 파일 하나. 첫 줄 `promptVersion: <값>` 다음이 본문이고 본문의 `{{이름}}` 이 빈칸이다.
 * 파일을 읽는 일과 나눠서 이 해석을 공통 테스트에서 본다.
 */
class PromptFile(val version: String, val body: String) {
    val placeholders: Set<String> = PLACEHOLDER.findAll(body).map { it.groupValues[1] }.toSet()

    /** 빈칸과 값의 이름이 하나라도 어긋나면 실패한다. 빠진 값이 모델에 `{{이름}}` 그대로 가지 않게 한다. */
    fun fill(values: Map<String, String>): String {
        require(values.keys == placeholders) { "빈칸 $placeholders 와 넣은 값 ${values.keys} 가 다르다" }
        return PLACEHOLDER.replace(body) { values.getValue(it.groupValues[1]) }
    }

    companion object {
        private const val VERSION_PREFIX = "promptVersion: "
        private val PLACEHOLDER = Regex("""\{\{(\w+)\}\}""")

        fun parse(text: String): PromptFile {
            val first = text.substringBefore('\n')
            require(first.startsWith(VERSION_PREFIX)) { "첫 줄이 '$VERSION_PREFIX' 로 시작해야 한다: $first" }
            return PromptFile(first.removePrefix(VERSION_PREFIX).trim(), text.substringAfter('\n').trim())
        }
    }
}

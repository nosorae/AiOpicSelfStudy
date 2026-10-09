package io.github.nosorae.aiopic.tools.eval

/** 시험 팩 프롬프트 파일 하나. 첫 줄 `promptVersion: <값>` 다음이 본문이고 `{{이름}}` 이 빈칸이다. 형식은 시험 팩의 PromptFile 과 같다. */
class PromptFile(val version: String, private val body: String) {
    fun fill(values: Map<String, String>): String = PLACEHOLDER.replace(body) { values[it.groupValues[1]] ?: error("빈칸 ${it.value} 에 넣을 값이 없다") }

    companion object {
        private val PLACEHOLDER = Regex("""\{\{(\w+)\}\}""")

        fun parse(text: String) = PromptFile(text.substringBefore('\n').removePrefix("promptVersion: ").trim(), text.substringAfter('\n').trim())
    }
}

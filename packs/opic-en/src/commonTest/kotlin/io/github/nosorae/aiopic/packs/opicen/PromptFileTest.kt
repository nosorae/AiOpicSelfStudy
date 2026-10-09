package io.github.nosorae.aiopic.packs.opicen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PromptFileTest {
    private val text = "promptVersion: opic-en-x-v1\n\nAnswer {{question}} as {{level}}.\n"

    @Test
    fun `첫 줄의 promptVersion 과 본문을 나눠 읽는다`() {
        val prompt = PromptFile.parse(text)

        assertEquals("opic-en-x-v1" to "Answer {{question}} as {{level}}.", prompt.version to prompt.body)
    }

    @Test
    fun `본문의 빈칸 이름을 모은다`() {
        assertEquals(setOf("question", "level"), PromptFile.parse(text).placeholders)
    }

    @Test
    fun `promptVersion 줄이 없으면 읽지 못한다`() {
        assertFailsWith<IllegalArgumentException> { PromptFile.parse("Answer {{question}}.") }
    }

    @Test
    fun `빈칸을 값으로 채운다`() {
        val filled = PromptFile.parse(text).fill(mapOf("question" to "Tell me about your house.", "level" to "5"))

        assertEquals("Answer Tell me about your house. as 5.", filled)
    }

    @Test
    fun `채우지 않은 빈칸이 있으면 실패한다`() {
        assertFailsWith<IllegalArgumentException> { PromptFile.parse(text).fill(mapOf("question" to "q")) }
    }

    @Test
    fun `본문에 없는 빈칸 값을 주면 실패한다`() {
        assertFailsWith<IllegalArgumentException> { PromptFile.parse(text).fill(mapOf("question" to "q", "level" to "1", "topic" to "home")) }
    }
}

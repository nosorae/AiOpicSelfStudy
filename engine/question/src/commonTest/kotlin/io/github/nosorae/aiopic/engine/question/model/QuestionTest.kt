package io.github.nosorae.aiopic.engine.question.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class QuestionTest {
    @Test
    fun `질문은 JSON 으로 저장했다 읽어도 같다`() {
        val question = Question(id = "q1", text = "Tell me about your house.", type = QuestionType("description"), topic = "home")

        val restored = Json.decodeFromString<Question>(Json.encodeToString(question))

        assertEquals(question, restored)
    }

    @Test
    fun `문항 유형은 JSON 에 문자열 하나로 들어간다`() {
        val json = Json.encodeToString(QuestionType("roleplay"))

        assertEquals("\"roleplay\"", json)
    }
}

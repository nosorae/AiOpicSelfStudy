package io.github.nosorae.aiopic.libs.llmgemini.api

import kotlin.test.Test
import kotlin.test.assertFalse

class LlmRequestTest {
    @Test
    fun `요청을 문자열로 찍어도 API 키가 나오지 않는다`() {
        val request = LlmRequest(apiKey = "test-key", model = "test-model", prompt = "Analyze.")

        assertFalse("test-key" in request.toString())
    }
}

package io.github.nosorae.aiopic.engine.answer.port

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class SavedResponsesTest {
    @Test
    fun `211 받은 원문은 시간 제한에 걸려도 끝까지 저장한다`() = runTest {
        val stored = mutableMapOf<String, String>()
        val saved = SavedResponses({ stored[it] }, { stage, text ->
            delay(1.seconds)
            stored[stage] = text
        })

        withTimeoutOrNull(0.5.seconds) { saved.save("answer", "raw") }

        assertEquals("raw", stored["answer"])
    }
}

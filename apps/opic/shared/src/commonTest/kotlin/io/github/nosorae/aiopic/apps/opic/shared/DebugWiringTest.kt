package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.apps.opic.shared.data.inMemoryDatabase
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.libs.llmgemini.api.LlmRequest
import io.github.nosorae.aiopic.libs.llmgemini.api.LlmResult
import io.github.nosorae.aiopic.libs.recorder.api.RecordingFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

class DebugWiringTest {
    private val request = LlmRequest(apiKey = "test-key", model = "m", prompt = "Reply with OK.")

    @Test
    fun `가짜 Gemini 는 보내지 않고 OK 를 돌려준다`() = runTest {
        assertEquals(LlmResult.Success("OK"), debugGeminiClient(fake = true, slow = { false }).generate(request))
    }

    @Test
    fun `느린 응답 스위치가 켜져 있으면 요청마다 기다린 뒤 보낸다`() = runTest {
        var slow = false
        val client = debugGeminiClient(fake = true, slow = { slow }, slowDelay = 300.milliseconds)

        // runTest 의 가상 시계는 delay 를 건너뛰어서 실제 시계로 잰다.
        val (fast, slowed) = withContext(Dispatchers.Default) {
            val fast = measureTime { client.generate(request) }
            slow = true
            fast to measureTime { client.generate(request) }
        }

        assertTrue(fast < 300.milliseconds, "fast=$fast")
        assertTrue(slowed >= 300.milliseconds, "slowed=$slowed")
    }

    @Test
    fun `녹음 실패 스위치가 켜져 있을 때만 멈추면 조각 쓰기 실패로 알린다`() = runTest {
        var fail = false
        val recorder = FakeRecorder.withDebugTools(fail = { fail })

        assertNull(recorder.stop().failure)
        fail = true
        assertEquals(RecordingFailure.WriteFailed, recorder.stop().failure)
    }

    @Test
    fun `설정 패널은 저장된 값을 보이고 문항 파일은 길이만 보인다`() = runTest {
        val settings = AppSettings.inMemory()
        settings.selectModel("gemini-x")
        settings.give()
        settings.saveQuestionFile("[1,2,3]")

        val rows = settings.debugRows().toMap()

        assertEquals("gemini-x", rows["model_id"])
        assertEquals("true", rows["voice_consent"])
        assertEquals("7 chars", rows["question_file"])
        assertEquals("(none)", rows["theme_mode"])
    }

    @Test
    fun `DB 패널은 표마다 행 수와 마지막 행을 보인다`() = runTest {
        val store = OpicStore(inMemoryDatabase())
        store.save(ExamSession("s1"))
        store.save(ExamSession("s2", stepIndex = 4))
        store.save(Answer("a1", Question("q1", "Q?", QuestionType("t"), "topic"), examSessionId = "s2", chunkPaths = emptyList()))
        store.mark(CorrectionFlag("a1", 0), wrong = true)

        assertEquals(
            listOf(
                "exam_session" to "2 rows, last s2 (step 4)",
                "answer" to "1 rows, last a1 (Pending)",
                "correction_flag" to "1 rows",
            ),
            dbRows(store),
        )
    }
}

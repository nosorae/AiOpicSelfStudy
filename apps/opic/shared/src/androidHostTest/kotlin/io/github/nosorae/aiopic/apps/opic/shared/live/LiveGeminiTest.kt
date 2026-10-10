package io.github.nosorae.aiopic.apps.opic.shared.live

import io.github.nosorae.aiopic.apps.opic.shared.AppSettings
import io.github.nosorae.aiopic.apps.opic.shared.FakeKeyVault
import io.github.nosorae.aiopic.apps.opic.shared.analysisQueueHandler
import io.github.nosorae.aiopic.apps.opic.shared.data.FakeWorkQueue
import io.github.nosorae.aiopic.apps.opic.shared.data.inMemoryDatabase
import io.github.nosorae.aiopic.engine.aigemini.GeminiModels
import io.github.nosorae.aiopic.engine.answer.analysis.AnalysisRequest
import io.github.nosorae.aiopic.engine.answer.analysis.GradeRequest
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.github.nosorae.aiopic.libs.workqueue.api.WorkResult
import io.github.nosorae.aiopic.packs.opicen.OpicEn
import io.github.nosorae.aiopic.packs.opicen.OpicPrompt
import io.github.nosorae.aiopic.packs.opicen.PromptFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 실제 Gemini 로 앱의 분석 경로가 이어지는지 본다(F5.2, F5.3). 요청 2번(답변 분석 1번, 회차 등급 1번)을 쓴다.
 * 키는 환경 변수 GEMINI_API_KEY 에서만 읽고, 없으면 건너뛰지 않고 실패한다. `./gradlew check` 에서는 빠지고
 * `--tests '*LiveGeminiTest*'` 로 부를 때만 돈다(빌드 파일의 제외 규칙).
 * 녹음은 macOS 기기 음성으로 만든 영어 답변(15초, 일부러 넣은 문법 오류 2개)이다. 프롬프트는 시험 팩 파일을 그대로 읽는다.
 * 호스트 JVM 테스트는 Compose 리소스를 못 읽어서 요청만 그래프의 조립 함수에 바꿔 넣는다. 채우는 값은 AppGraph 의 요청 함수와 같다.
 */
class LiveGeminiTest {
    private val db = inMemoryDatabase()

    @AfterTest fun close() = db.close()

    @Test
    fun `F5_2 F5_3 실제 Gemini 로 녹음 하나를 분석해 Done 으로 저장하고 회차 등급을 저장한다`() = runBlocking {
        val key = assertNotNull(System.getenv("GEMINI_API_KEY")?.takeIf { it.isNotBlank() }, "GEMINI_API_KEY 환경 변수가 필요하다")
        val settings = AppSettings.inMemory().apply { give() }
        val keyVault = FakeKeyVault().apply { put(GeminiModels.PROVIDER, key) }
        val queue = FakeWorkQueue()
        val store = OpicStore(db)
        val handler = analysisQueueHandler(
            store = store,
            queue = queue,
            client = GeminiClient(),
            keyVault = keyVault,
            settings = settings,
            answerRequest = { answer, model ->
                val file = prompt("answer.md")
                val text = file.fill(mapOf("question" to answer.question.text, "questionType" to answer.question.type.id, "resumedAt" to "none"))
                AnalysisRequest(model, Prompt(file.version, text, OpicPrompt.Answer.responseSchema.toString()), recording)
            },
            gradeRequest = { answers, model ->
                val file = prompt("session-grade.md")
                val text = file.fill(mapOf("answers" to Json.encodeToString(answers.map { it.analysis })))
                GradeRequest(model, Prompt(file.version, text, OpicPrompt.SessionGrade.responseSchema.toString()), OpicEn.gradeScale, answers.map { recording })
            },
        )
        store.save(ExamSession("s1", answerIds = listOf("a1"), status = ExamStatus.Finished))
        store.save(Answer("a1", Question("q1", "Tell me about your home. What does it look like?", QuestionType("description"), "home"), "s1", chunkPaths = emptyList()))
        queue.enqueue("a1", "a1")

        var next = 0
        while (next < queue.ids.size) {
            val id = queue.ids[next++]
            // 429, 503 은 Retry 로 온다. 앱은 나중에 다시 보내지만 이 테스트는 호출 수를 지키려고 다시 보내지 않는다.
            assertEquals(WorkResult.Success, handler.handle(id), "$id 처리 결과")
        }

        assertEquals(listOf("a1", "grade:s1"), queue.ids)
        val analysis = assertIs<AnswerAnalysis.Done>(store.answers("s1").first().single().analysis)
        assertTrue(analysis.feedback.transcript.isNotBlank())
        val grade = assertNotNull(store.session("s1").first()?.grade)
        val names = OpicEn.gradeScale.grades
        assertTrue(grade.low in names && grade.high in names, "${grade.low} ~ ${grade.high}")
    }

    private val recording = Recording(File(DIR, "answer.m4a").readBytes(), "audio/mp4", emptyList())

    private fun prompt(name: String) = PromptFile.parse(File(PROMPTS, name).readText())
}

// 테스트는 모듈 폴더에서 돈다.
private const val DIR = "src/androidHostTest/kotlin/io/github/nosorae/aiopic/apps/opic/shared/live"
private const val PROMPTS = "../../../packs/opic-en/src/commonMain/composeResources/files/prompts"

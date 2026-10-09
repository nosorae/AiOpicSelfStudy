package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.apps.opic.shared.data.inMemoryDatabase
import io.github.nosorae.aiopic.engine.aigemini.GeminiModels
import io.github.nosorae.aiopic.engine.answer.analysis.AnalysisRequest
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.github.nosorae.aiopic.libs.workqueue.api.WorkResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelFallBackTest {
    private val db = inMemoryDatabase()
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun close() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `F5_8 고른 모델이 없다는 답이 오면 기본 모델로 바꾸고 다시 보내며 설정 화면에 한 번 알린다`() = runTest(dispatcher) {
        val selected = GeminiModels.models.first { it != GeminiModels.defaultModel }.id
        val requested = mutableListOf<String>()
        // 고른 모델은 404(모델 없음), 기본 모델은 503(잠시 못 씀)이라 분석 결과 JSON 없이 어느 모델로 갔는지만 본다.
        val client = GeminiClient(
            HttpClient(
                MockEngine { request ->
                    val path = request.url.encodedPath
                    requested += path
                    respond("{}", if (selected in path) HttpStatusCode.NotFound else HttpStatusCode.ServiceUnavailable)
                },
            ),
        )
        val settings = AppSettings.inMemory().apply {
            give()
            selectModel(selected)
        }
        val keyVault = FakeKeyVault().apply { put(GeminiModels.PROVIDER, "test-key") }
        val store = OpicStore(db).apply { save(Answer("a1", Question("q1", "Tell me about your home.", QuestionType("intro"), "home"), null, chunkPaths = emptyList())) }
        val handler = answerAnalysisHandler(store, client, keyVault, settings) { _, model ->
            AnalysisRequest(model, Prompt("test", "prompt", "{}"), Recording(ByteArray(0), "audio/mp4", emptyList()))
        }

        assertEquals(WorkResult.Retry, handler.handle("a1"))
        assertEquals(GeminiModels.defaultModel.id, settings.selectedModelId())

        assertEquals(WorkResult.Retry, handler.handle("a1"))
        assertEquals(2, requested.size)
        assertTrue(GeminiModels.defaultModel.id in requested[1])

        val screen = testAppGraph(settings).settingsViewModel
        advanceUntilIdle()
        assertEquals(selected, screen.uiState.value.fellBackFrom)
        assertNull(settings.takeFellBackFrom())
    }
}

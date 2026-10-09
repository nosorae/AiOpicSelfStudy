package io.github.nosorae.aiopic.ui.featurehistory

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.subscribed(store: FakeHistoryStore) = HistoryViewModel(ObserveHistoryUseCase(store)).also { viewModel ->
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun `F7_3 끝난 회차만 저장한 순서대로 기록에 올린다`() = runTest(dispatcher) {
        val sessions = listOf(
            ExamSession(id = "s1", status = ExamStatus.Finished),
            ExamSession(id = "s2", status = ExamStatus.InProgress),
            ExamSession(id = "s3", status = ExamStatus.TimeUp),
        )

        val viewModel = subscribed(FakeHistoryStore(sessions))
        advanceUntilIdle()

        assertEquals(listOf("s1", "s3"), viewModel.uiState.value.sessions.map { it.session.id })
    }

    @Test
    fun `F5_7 회차마다 분석한 모델을 모으고 섞였으면 표시한다`() = runTest(dispatcher) {
        val store = FakeHistoryStore(
            sessions = listOf(ExamSession(id = "s1", status = ExamStatus.Finished), ExamSession(id = "s2", status = ExamStatus.Finished)),
            answers = listOf(answer("a1", "s1", "model-a"), answer("a2", "s1", "model-b"), answer("a3", "s2", "model-a"), answer("a4", "s2", "model-a")),
        )

        val viewModel = subscribed(store)
        advanceUntilIdle()

        val (first, second) = viewModel.uiState.value.sessions
        assertTrue(first.mixedModels)
        assertEquals(listOf("model-a"), second.models.map { it.modelId })
        assertFalse(second.mixedModels)
    }

    @Test
    fun `F7_3 한 문항 연습까지 모든 답변으로 문항 유형별 약점을 약한 것부터 낸다`() = runTest(dispatcher) {
        val store = FakeHistoryStore(
            answers = listOf(answer("a1", "s1", score = 80, type = "routine"), answer("a2", null, score = 40, type = "roleplay")),
        )

        val viewModel = subscribed(store)
        advanceUntilIdle()

        assertEquals(listOf("roleplay" to 40, "routine" to 80), viewModel.uiState.value.weaknesses.map { it.type.id to it.score })
    }

    @Test
    fun `F8_2 틀림을 표시하면 그 답변이 약점 점수에서 빠진다`() = runTest(dispatcher) {
        val store = FakeHistoryStore(answers = listOf(answer("a1", "s1", score = 40), answer("a2", "s1", score = 80)))
        val viewModel = subscribed(store)
        advanceUntilIdle()

        store.flags.value = setOf(CorrectionFlag("a1", 0))
        advanceUntilIdle()

        assertEquals(listOf("t" to 80), viewModel.uiState.value.weaknesses.map { it.type.id to it.score })
    }

    @Test
    fun `불러오기 전에는 빈 기록 대신 불러오는 중이다`() = runTest(dispatcher) {
        val viewModel = HistoryViewModel(ObserveHistoryUseCase(FakeHistoryStore()))

        assertTrue(viewModel.uiState.value.loading)
    }

    private fun answer(id: String, sessionId: String?, model: String = "model-a", score: Int = 70, type: String = "t") = Answer(
        id = id,
        question = Question("q", "text", QuestionType(type), "topic"),
        examSessionId = sessionId,
        chunkPaths = emptyList(),
        analysis = AnswerAnalysis.Done(
            AnalyzedBy("gemini", model, "v1"),
            Feedback("I go.", listOf(Correction("I go", "I went")), emptyList(), true, 1, 100.0, emptyList(), null, Pronunciation.NotEvaluated, score),
        ),
    )
}

package io.github.nosorae.aiopic.ui.featurehistory

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.GradeScale
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

    private val modelA = ModelInfo("gemini", "model-a", "Model A", hearsAudio = true, stability = Stability.Stable)
    private val catalog = object : ModelCatalog {
        override val defaultModel = modelA
        override val models = listOf(modelA)
    }
    private val typeNames = mapOf(QuestionType("routine") to "습관", QuestionType("roleplay") to "롤플레이")

    private fun useCase(store: FakeHistoryStore) = ObserveHistoryUseCase(store, typeNames, catalog, GradeScale(listOf("NL", "IM", "AL")))

    private fun TestScope.subscribed(store: FakeHistoryStore) = HistoryViewModel(useCase(store)).also { viewModel ->
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun `F7_3 끝난 회차만 최근 것부터 기록에 올린다`() = runTest(dispatcher) {
        val sessions = listOf(
            ExamSession(id = "s1", status = ExamStatus.Finished),
            ExamSession(id = "s2", status = ExamStatus.InProgress),
            ExamSession(id = "s3", status = ExamStatus.TimeUp),
        )

        val viewModel = subscribed(FakeHistoryStore(sessions))
        advanceUntilIdle()

        assertEquals(listOf("s3", "s1"), viewModel.uiState.value.sessions.map { it.session.id })
        assertEquals(listOf(2, 1), viewModel.uiState.value.sessions.map { it.number })
    }

    @Test
    fun `F5_7 회차마다 분석한 모델을 모으고 섞였으면 표시한다`() = runTest(dispatcher) {
        val store = FakeHistoryStore(
            sessions = listOf(ExamSession(id = "s1", status = ExamStatus.Finished), ExamSession(id = "s2", status = ExamStatus.Finished)),
            answers = listOf(answer("a1", "s1", "model-a"), answer("a2", "s1", "model-b"), answer("a3", "s2", "model-a"), answer("a4", "s2", "model-a")),
        )

        val viewModel = subscribed(store)
        advanceUntilIdle()

        val (second, first) = viewModel.uiState.value.sessions
        assertTrue(first.mixedModels)
        assertEquals(listOf("Model A"), second.models)
        assertFalse(second.mixedModels)
    }

    @Test
    fun `F5_7 모델은 표시 이름으로 보이고 목록에 없는 모델만 id 로 보인다`() = runTest(dispatcher) {
        val store = FakeHistoryStore(
            sessions = listOf(ExamSession(id = "s1", status = ExamStatus.Finished)),
            answers = listOf(answer("a1", "s1", "model-a"), answer("a2", "s1", "retired-model")),
        )

        val viewModel = subscribed(store)
        advanceUntilIdle()

        assertEquals(listOf("Model A", "retired-model"), viewModel.uiState.value.sessions.single().models)
    }

    @Test
    fun `F7_3 한 문항 연습까지 모든 답변으로 문항 유형별 약점을 약한 것부터 낸다`() = runTest(dispatcher) {
        val store = FakeHistoryStore(
            answers = listOf(answer("a1", "s1", score = 80, type = "routine"), answer("a2", null, score = 40, type = "roleplay")),
        )

        val viewModel = subscribed(store)
        advanceUntilIdle()

        assertEquals(listOf(HistoryWeakness("롤플레이", 40), HistoryWeakness("습관", 80)), viewModel.uiState.value.weaknesses)
    }

    @Test
    fun `F7_4 회차와 한 문항 연습 답변을 저장한 순서로 섞어 최근 것부터 보인다`() = runTest(dispatcher) {
        val store = FakeHistoryStore(
            sessions = listOf(ExamSession(id = "s1", status = ExamStatus.Finished), ExamSession(id = "s2", status = ExamStatus.Finished)),
            answers = listOf(answer("p1", null), answer("a1", "s1"), answer("a2", "s1"), answer("p2", null), answer("a3", "s2")),
        )

        val viewModel = subscribed(store)
        advanceUntilIdle()

        val ids = viewModel.uiState.value.entries.map {
            when (it) {
                is HistorySession -> it.session.id
                is HistoryPractice -> it.answerId
            }
        }
        assertEquals(listOf("s2", "p2", "s1", "p1"), ids)
    }

    @Test
    fun `F7_4 연습 답변은 문항 유형 이름과 분석 상태와 모델을 가진다`() = runTest(dispatcher) {
        val failed = answer("p2", null, type = "roleplay").copy(analysis = AnswerAnalysis.Failed(AnalysisError.Retryable))
        val store = FakeHistoryStore(answers = listOf(answer("p1", null, score = 72, type = "routine"), failed))

        val viewModel = subscribed(store)
        advanceUntilIdle()

        val (second, first) = viewModel.uiState.value.entries.map { it as HistoryPractice }
        assertEquals("습관", first.typeName)
        assertEquals("Model A", first.model)
        assertEquals(72, (first.analysis as AnswerAnalysis.Done).feedback.score)
        assertEquals("롤플레이", second.typeName)
        assertEquals(null, second.model)
        assertTrue(second.analysis is AnswerAnalysis.Failed)
    }

    @Test
    fun `회차 답변이 없는 회차는 앞 회차 바로 뒤에 둔다`() = runTest(dispatcher) {
        val store = FakeHistoryStore(
            sessions = listOf(ExamSession(id = "s1", status = ExamStatus.Finished), ExamSession(id = "s2", status = ExamStatus.TimeUp)),
            answers = listOf(answer("a1", "s1"), answer("p1", null)),
        )

        val viewModel = subscribed(store)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.entries.size)
        assertTrue(viewModel.uiState.value.entries.first() is HistoryPractice)
        assertEquals(listOf("s2", "s1"), viewModel.uiState.value.sessions.map { it.session.id })
    }

    @Test
    fun `이름을 모르는 문항 유형은 id 로 보인다`() = runTest(dispatcher) {
        val viewModel = subscribed(FakeHistoryStore(answers = listOf(answer("a1", "s1", type = "new-type"))))
        advanceUntilIdle()

        assertEquals(listOf("new-type"), viewModel.uiState.value.weaknesses.map { it.typeName })
    }

    @Test
    fun `F8_2 틀림을 표시하면 그 답변이 약점 점수에서 빠진다`() = runTest(dispatcher) {
        val store = FakeHistoryStore(answers = listOf(answer("a1", "s1", score = 40), answer("a2", "s1", score = 80)))
        val viewModel = subscribed(store)
        advanceUntilIdle()

        store.flags.value = setOf(CorrectionFlag("a1", 0))
        advanceUntilIdle()

        assertEquals(listOf(HistoryWeakness("t", 80)), viewModel.uiState.value.weaknesses)
    }

    @Test
    fun `불러오기 전에는 빈 기록 대신 불러오는 중이다`() = runTest(dispatcher) {
        val viewModel = HistoryViewModel(useCase(FakeHistoryStore()))

        assertTrue(viewModel.uiState.value.loading)
    }

    @Test
    fun `등급을 기다리는 회차는 답한 수와 분석이 끝난 수를 센다`() = runTest(dispatcher) {
        val store = FakeHistoryStore(
            sessions = listOf(ExamSession(id = "s1", status = ExamStatus.Finished)),
            answers = listOf(answer("a1", "s1"), answer("a2", "s1").copy(analysis = AnswerAnalysis.Pending), answer("a3", "other")),
        )

        val viewModel = subscribed(store)
        advanceUntilIdle()

        val row = viewModel.uiState.value.sessions.single()
        assertEquals(2, row.answered)
        assertEquals(1, row.analyzed)
    }

    @Test
    fun `등급 눈금은 시험 팩의 등급 이름을 낮은 것부터 쓴다`() = runTest(dispatcher) {
        val viewModel = subscribed(FakeHistoryStore(sessions = listOf(ExamSession(id = "s1", status = ExamStatus.Finished))))
        advanceUntilIdle()

        assertEquals(listOf("NL", "IM", "AL"), viewModel.uiState.value.grades)
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

package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.JobKind
import io.github.nosorae.aiopic.engine.answer.model.JobState
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.answer.weakness.TypeWeakness
import io.github.nosorae.aiopic.engine.answer.weakness.weaknessScores
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.ui.featurereport.flag.FakeFlagStore
import io.github.nosorae.aiopic.ui.featurereport.flag.MarkCorrectionWrongUseCase
import io.github.nosorae.aiopic.ui.featurereport.flag.ObserveAnswerReportUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant

class AnswerReportViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val flagStore = FakeFlagStore()

    private fun viewModel(answerId: String, store: FakeReportStore) = AnswerReportViewModel(
        answerId,
        ObserveAnswerReportUseCase(ObserveAnswerUseCase(store), flagStore, store, FakeCatalog),
        MarkCorrectionWrongUseCase(flagStore),
        RetryAnswerAnalysisUseCase(store),
    )

    /** a0 은 40점에 교정 둘, a1 은 80점에 교정 하나. 문항 유형이 같다. */
    private fun corrected() = answers("s1", 2).mapIndexed { i, answer ->
        val analysis = done(score = if (i == 0) 40 else 80)
        val corrections = List(2 - i) { Correction("I go", "I went") }
        answer.copy(analysis = analysis.copy(feedback = analysis.feedback.copy(corrections = corrections)))
    }

    @Test
    fun `F7_1 한 문항 연습 답변도 같은 화면에서 보인다`() = runTest(dispatcher) {
        val store = FakeReportStore(answers = answers(examSessionId = null, count = 2))

        val viewModel = viewModel("a1", store)
        advanceUntilIdle()

        assertEquals("a1", viewModel.uiState.value.answer?.id)
    }

    @Test
    fun `F5_7 답변 리포트도 모델 표시 이름을 보인다`() = runTest(dispatcher) {
        val viewModel = viewModel("a0", FakeReportStore(answers = answers("s1", 1)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val analyzedBy = assertIs<AnswerAnalysis.Done>(state.answer?.analysis).analyzedBy
        assertEquals("Model A", state.names.model(analyzedBy))
    }

    @Test
    fun `분석이 끝나면 대기 화면이 피드백으로 바뀐다`() = runTest(dispatcher) {
        val store = FakeReportStore(answers = answers("s1", 1).map { it.copy(analysis = AnswerAnalysis.Pending) })
        val viewModel = viewModel("a0", store)
        advanceUntilIdle()

        store.answers.value = answers("s1", 1)
        advanceUntilIdle()

        assertIs<AnswerAnalysis.Done>(viewModel.uiState.value.answer?.analysis)
    }

    @Test
    fun `205 기다리는 답변은 분석 작업 상태가 바뀌는 대로 보인다`() = runTest(dispatcher) {
        val store = FakeReportStore(answers = answers("s1", 1).map { it.copy(analysis = AnswerAnalysis.Pending) })
        val viewModel = viewModel("a0", store)
        store.jobs.value = listOf(AnalysisJob(JobKind.Answer, "a0", JobState.WaitingNetwork, updatedAt = Instant.fromEpochMilliseconds(0)))
        advanceUntilIdle()
        assertEquals(JobState.WaitingNetwork, viewModel.uiState.value.job?.state)

        store.jobs.value = listOf(AnalysisJob(JobKind.Answer, "a0", JobState.Running, updatedAt = Instant.fromEpochMilliseconds(1)), AnalysisJob(JobKind.Answer, "other", JobState.Failed, updatedAt = Instant.fromEpochMilliseconds(1)))
        advanceUntilIdle()
        assertEquals(JobState.Running, viewModel.uiState.value.job?.state)
    }

    @Test
    fun `203 분석하지 못한 답변에서 다시 분석하기를 누르면 그 답변을 다시 보낸다`() = runTest(dispatcher) {
        val store = FakeReportStore(answers = answers("s1", 1).map { it.copy(analysis = AnswerAnalysis.Failed(AnalysisError.Retryable)) })
        val viewModel = viewModel("a0", store)
        advanceUntilIdle()

        viewModel.onIntent(AnswerReportIntent.RetryAnalysis)
        advanceUntilIdle()

        assertEquals(listOf("a0"), store.retried)
    }

    @Test
    fun `없는 답변이면 불러오기를 끝내고 비운다`() = runTest(dispatcher) {
        val viewModel = viewModel("none", FakeReportStore())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loading)
        assertNull(viewModel.uiState.value.answer)
    }

    @Test
    fun `F8_1 교정마다 틀림을 누르면 표시되고 다시 누르면 풀린다`() = runTest(dispatcher) {
        val viewModel = viewModel("a0", FakeReportStore(answers = corrected()))
        advanceUntilIdle()

        viewModel.onIntent(AnswerReportIntent.ToggleCorrectionWrong(1))
        advanceUntilIdle()
        assertEquals(setOf(1), viewModel.uiState.value.wrongCorrections)
        assertEquals(setOf(CorrectionFlag("a0", 1)), flagStore.flags.value)

        viewModel.onIntent(AnswerReportIntent.ToggleCorrectionWrong(1))
        advanceUntilIdle()
        assertEquals(emptySet(), viewModel.uiState.value.wrongCorrections)
    }

    @Test
    fun `다른 답변의 표시는 이 답변 화면에 보이지 않는다`() = runTest(dispatcher) {
        flagStore.flags.value = setOf(CorrectionFlag("a1", 0))

        val viewModel = viewModel("a0", FakeReportStore(answers = corrected()))
        advanceUntilIdle()

        assertEquals(emptySet(), viewModel.uiState.value.wrongCorrections)
    }

    @Test
    fun `F8_2 틀림을 누른 답변은 약점 점수에서 빠진다`() = runTest(dispatcher) {
        val store = FakeReportStore(answers = corrected())
        assertEquals(listOf(TypeWeakness(QuestionType("t"), 60)), weaknessScores(store.answers.value, flagStore.flags.value))

        val viewModel = viewModel("a0", store)
        advanceUntilIdle()
        viewModel.onIntent(AnswerReportIntent.ToggleCorrectionWrong(0))
        advanceUntilIdle()

        assertEquals(listOf(TypeWeakness(QuestionType("t"), 80)), weaknessScores(store.answers.value, flagStore.flags.value))
    }
}

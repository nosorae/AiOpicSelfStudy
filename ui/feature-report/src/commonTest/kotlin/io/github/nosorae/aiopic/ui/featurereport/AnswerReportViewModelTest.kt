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
import kotlinx.coroutines.CompletableDeferred
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

    private val tts = FakeTts()

    private fun viewModel(answerId: String, store: FakeReportStore, voice: ScriptVoice = FakeScriptVoice()) = AnswerReportViewModel(
        answerId,
        ObserveAnswerReportUseCase(ObserveAnswerUseCase(store), flagStore, store, FakeCatalog),
        MarkCorrectionWrongUseCase(flagStore),
        RetryAnswerAnalysisUseCase(store),
        ListenScriptUseCase(voice, tts),
        CelebrateOnceUseCase(store),
    )

    private fun pending(count: Int = 2) = answers("s1", count).map { it.copy(analysis = AnswerAnalysis.Pending) }

    @Test
    fun `198 보는 중에 첫 답변 분석이 끝나면 작은 축하를 하고 다음 답변부터는 하지 않는다`() = runTest(dispatcher) {
        val store = FakeReportStore(answers = pending())
        val first = viewModel("a0", store)
        val second = viewModel("a1", store)
        advanceUntilIdle()
        assertFalse(first.uiState.value.celebrateAnalysis)

        store.answers.value = answers("s1", 2)
        advanceUntilIdle()

        assertEquals(listOf(true, false), listOf(first, second).map { it.uiState.value.celebrateAnalysis })
    }

    @Test
    fun `198 첫 분석 축하를 본 답변을 다시 열면 축하하지 않는다`() = runTest(dispatcher) {
        val store = FakeReportStore(answers = pending(1))
        viewModel("a0", store)
        advanceUntilIdle()
        store.answers.value = answers("s1", 1)
        advanceUntilIdle()

        val again = viewModel("a0", store)
        advanceUntilIdle()

        assertFalse(again.uiState.value.celebrateAnalysis)
    }

    @Test
    fun `198 첫 분석 축하는 앱을 다시 켜도 다시 터지지 않는다`() = runTest(dispatcher) {
        val before = FakeReportStore(answers = pending())
        viewModel("a0", before)
        advanceUntilIdle()
        before.answers.value = answers("s1", 2)
        advanceUntilIdle()

        val restarted = FakeReportStore(answers = pending(), seen = before.seen.toMutableMap())
        val viewModel = viewModel("a1", restarted)
        advanceUntilIdle()
        restarted.answers.value = answers("s1", 2)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.celebrateAnalysis)
    }

    @Test
    fun `198 이미 분석된 답변을 처음 열면 축하하지 않는다`() = runTest(dispatcher) {
        val viewModel = viewModel("a0", FakeReportStore(answers = answers("s1", 1)))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.celebrateAnalysis)
    }

    private fun scripted() = answers("s1", 1).map { answer ->
        val analysis = done()
        answer.copy(analysis = analysis.copy(feedback = analysis.feedback.copy(finalScript = "I went to the park.")))
    }

    @Test
    fun `217 최종 스크립트 듣기를 누르면 자연스러운 음성을 받는 동안 준비 중이고 읽는 동안 듣는 중이다`() = runTest(dispatcher) {
        val voice = FakeScriptVoice(ready = CompletableDeferred())
        val viewModel = viewModel("a0", FakeReportStore(answers = scripted()), voice)
        advanceUntilIdle()

        viewModel.onIntent(AnswerReportIntent.ListenScript)
        advanceUntilIdle()
        assertEquals(ScriptPlayback.Preparing, viewModel.uiState.value.scriptPlayback)
        voice.ready.complete(Unit)
        advanceUntilIdle()
        assertEquals(ScriptPlayback.Playing, viewModel.uiState.value.scriptPlayback)
        tts.finish()
        advanceUntilIdle()

        assertEquals(ScriptPlayback.Idle, viewModel.uiState.value.scriptPlayback)
        assertEquals(listOf("I went to the park."), voice.asked)
        assertEquals("I went to the park." to 1, tts.spoken.single().let { it.first to it.second?.size })
    }

    @Test
    fun `217 듣는 중에 다시 누르면 읽기를 멈춘다`() = runTest(dispatcher) {
        val viewModel = viewModel("a0", FakeReportStore(answers = scripted()))
        advanceUntilIdle()
        viewModel.onIntent(AnswerReportIntent.ListenScript)
        advanceUntilIdle()

        viewModel.onIntent(AnswerReportIntent.ListenScript)
        advanceUntilIdle()

        assertEquals(ScriptPlayback.Idle, viewModel.uiState.value.scriptPlayback)
        assertEquals(1, tts.spoken.size)
    }

    @Test
    fun `217 자연스러운 음성을 받지 못하면 기기 음성으로 읽는다`() = runTest(dispatcher) {
        val viewModel = viewModel("a0", FakeReportStore(answers = scripted()), FakeScriptVoice(audio = null))
        advanceUntilIdle()

        viewModel.onIntent(AnswerReportIntent.ListenScript)
        advanceUntilIdle()

        assertEquals("I went to the park." to null, tts.spoken.single())
    }

    @Test
    fun `217 최종 스크립트를 복사하면 복사했다는 표시가 남는다`() = runTest(dispatcher) {
        val viewModel = viewModel("a0", FakeReportStore(answers = scripted()))
        advanceUntilIdle()

        viewModel.onIntent(AnswerReportIntent.ScriptCopied)

        assertEquals(true, viewModel.uiState.value.scriptCopied)
    }

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

package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.GradeScale
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class SessionReportViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    /** 수준 3 이상은 5문항, 1~2 는 마지막 하나를 건너뛰어 4문항이다. */
    private val spec = ExamSpec(
        timeLimit = 40.minutes,
        levelCount = 6,
        steps = listOf(ExamStep.LevelSelect) + List(4) { ExamStep.Ask(QuestionType("t")) } + ExamStep.Ask(QuestionType("t"), minLevel = 3),
    )

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(store: FakeReportStore) = SessionReportViewModel("s1", ObserveSessionReportUseCase(store, spec, typeNames, FakeCatalog, GradeScale(listOf("NL", "IM", "AL"))))

    @Test
    fun `F2_6 시간이 다 된 회차는 답하지 못한 문항 수를 시간 부족으로 센다`() = runTest(dispatcher) {
        val store = FakeReportStore(listOf(session(level = 3, answers = 3, status = ExamStatus.TimeUp)), answers("s1", 3))

        val viewModel = viewModel(store)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.report?.timeShortCount)
    }

    @Test
    fun `수준 1과 2 회차는 건너뛴 문항을 시간 부족으로 세지 않는다`() = runTest(dispatcher) {
        val store = FakeReportStore(listOf(session(level = 2, answers = 3, status = ExamStatus.TimeUp)), answers("s1", 3))

        val viewModel = viewModel(store)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.report?.timeShortCount)
    }

    @Test
    fun `끝까지 마친 회차에는 시간 부족 문항이 없다`() = runTest(dispatcher) {
        val store = FakeReportStore(listOf(session(level = 3, answers = 5)), answers("s1", 5))

        val viewModel = viewModel(store)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.report?.timeShortCount)
    }

    @Test
    fun `F7_2 답변 목록은 회차에서 답한 순서대로다`() = runTest(dispatcher) {
        val store = FakeReportStore(listOf(session(level = 3, answers = 3).copy(answerIds = listOf("a2", "a0", "a1"))), answers("s1", 3))

        val viewModel = viewModel(store)
        advanceUntilIdle()

        assertEquals(listOf("a2", "a0", "a1"), viewModel.uiState.value.report?.answers?.map { it.id })
    }

    @Test
    fun `F5_7 한 회차 답변을 두 모델이 분석했으면 섞였다고 표시한다`() = runTest(dispatcher) {
        val list = answers("s1", 3).mapIndexed { i, a -> if (i == 2) a.copy(analysis = done(model = "model-b")) else a }
        val store = FakeReportStore(listOf(session(level = 3, answers = 3)), list)

        val viewModel = viewModel(store)
        advanceUntilIdle()

        val report = viewModel.uiState.value.report!!
        assertTrue(report.mixedModels)
        assertEquals(listOf("model-a", "model-b"), report.models.map { it.modelId })
    }

    @Test
    fun `F5_7 모델은 표시 이름으로 보이고 목록에 없는 모델만 id 로 보인다`() = runTest(dispatcher) {
        val list = answers("s1", 2).mapIndexed { i, a -> if (i == 1) a.copy(analysis = done(model = "model-b")) else a }
        val viewModel = viewModel(FakeReportStore(listOf(session(level = 3, answers = 2)), list))
        advanceUntilIdle()

        val report = viewModel.uiState.value.report!!
        assertEquals(listOf("Model A", "model-b"), report.models.map(report.names::model))
    }

    @Test
    fun `답변 목록은 문항 유형 id 대신 시험 팩이 정한 이름을 보이고 모르는 유형만 id 를 보인다`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeReportStore(listOf(session(level = 3, answers = 1)), answers("s1", 1)))
        advanceUntilIdle()

        val names = viewModel.uiState.value.report!!.names
        assertEquals("유형 T", names.type(QuestionType("t")))
        assertEquals("new-type", names.type(QuestionType("new-type")))
    }

    @Test
    fun `F5_7 분석 대기 답변은 모델 섞임 판단에서 뺀다`() = runTest(dispatcher) {
        val list = answers("s1", 3).mapIndexed { i, a -> if (i == 2) a.copy(analysis = AnswerAnalysis.Pending) else a }
        val store = FakeReportStore(listOf(session(level = 3, answers = 3)), list)

        val viewModel = viewModel(store)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.report!!.mixedModels)
    }

    @Test
    fun `답변 분석이 끝나면 리포트가 바로 바뀐다`() = runTest(dispatcher) {
        val pending = answers("s1", 1).map { it.copy(analysis = AnswerAnalysis.Pending) }
        val store = FakeReportStore(listOf(session(level = 3, answers = 1)), pending)
        val viewModel = viewModel(store)
        advanceUntilIdle()

        store.answers.value = answers("s1", 1)
        advanceUntilIdle()

        assertEquals(listOf("model-a"), viewModel.uiState.value.report?.models?.map { it.modelId })
    }

    @Test
    fun `없는 회차면 불러오기를 끝내고 리포트를 비운다`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeReportStore())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loading)
        assertNull(viewModel.uiState.value.report)
    }

    @Test
    fun `등급 눈금은 시험 팩의 등급 이름을 낮은 것부터 쓴다`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeReportStore(listOf(session(level = 3, answers = 1)), answers("s1", 1)))
        advanceUntilIdle()

        assertEquals(listOf("NL", "IM", "AL"), viewModel.uiState.value.report!!.names.grades)
    }

    @Test
    fun `F5_3 등급을 기다리는 동안 답한 수 중 분석이 끝난 수를 센다`() = runTest(dispatcher) {
        val list = answers("s1", 3).mapIndexed { i, a -> if (i == 2) a.copy(analysis = AnswerAnalysis.Pending) else a }
        val viewModel = viewModel(FakeReportStore(listOf(session(level = 3, answers = 3)), list))
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.report!!.analyzedCount)
    }
}

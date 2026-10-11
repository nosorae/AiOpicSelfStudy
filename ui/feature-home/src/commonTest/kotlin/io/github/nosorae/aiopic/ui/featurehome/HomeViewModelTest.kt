package io.github.nosorae.aiopic.ui.featurehome

import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.GradeCheck
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessions = MutableStateFlow<List<ExamSession>>(emptyList())
    private val source = object : ExamSessionSource {
        override fun sessions() = sessions

        override suspend fun save(session: ExamSession) = sessions.update { all -> all.map { if (it.id == session.id) session else it } }
    }

    // 수준 1 이면 3문항, 수준 3 이상이면 4문항인 시험.
    private val spec = ExamSpec(
        timeLimit = 40.minutes,
        levelCount = 6,
        steps = listOf(ExamStep.LevelSelect) + List(3) { ExamStep.Ask(QuestionType("t")) } + ExamStep.Ask(QuestionType("t"), minLevel = 3),
    )

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.loadedViewModel(): HomeViewModel {
        val viewModel = HomeViewModel(ObserveHomeUseCase(source, spec), StartNewExamUseCase(source))
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `저장소를 읽기 전에는 지난 회차가 정해지지 않은 로딩 상태다`() {
        val viewModel = HomeViewModel(ObserveHomeUseCase(source, spec), StartNewExamUseCase(source))

        assertNull(viewModel.uiState.value.pastSessions)
    }

    @Test
    fun `F3 진행 중인 회차가 있으면 그 회차를 이어 본다`() = runTest(dispatcher) {
        sessions.value = listOf(ExamSession(id = "a", status = ExamStatus.Finished), ExamSession(id = "b", status = ExamStatus.InProgress))

        val viewModel = loadedViewModel()

        assertEquals("b", viewModel.uiState.value.resumeSessionId)
        assertTrue(viewModel.uiState.value.canResume)
    }

    @Test
    fun `F3 이어 볼 회차가 고른 수준의 문항 중 몇 개를 답했는지 보인다`() = runTest(dispatcher) {
        sessions.value = listOf(ExamSession(id = "b", level = 3, answerIds = listOf("x", "y"), status = ExamStatus.InProgress))

        val viewModel = loadedViewModel()

        assertEquals(2 to 4, viewModel.uiState.value.resumeDone to viewModel.uiState.value.resumeTotal)
    }

    @Test
    fun `진행 중인 회차가 없으면 새로 시작한다`() = runTest(dispatcher) {
        sessions.value = listOf(ExamSession(id = "a", status = ExamStatus.Finished))

        val viewModel = loadedViewModel()

        assertFalse(viewModel.uiState.value.canResume)
    }

    @Test
    fun `지난 회차는 끝난 회차만 최근 것부터 회차 번호와 함께 보인다`() = runTest(dispatcher) {
        sessions.value = listOf(
            ExamSession(id = "a", status = ExamStatus.Finished),
            ExamSession(id = "b", status = ExamStatus.TimeUp),
            ExamSession(id = "c", status = ExamStatus.InProgress),
        )

        val viewModel = loadedViewModel()

        assertEquals(listOf("b" to 2, "a" to 1), viewModel.uiState.value.pastSessions?.map { it.id to it.number })
    }

    @Test
    fun `등급이 나온 회차는 등급 범위를 보인다`() = runTest(dispatcher) {
        sessions.value = listOf(ExamSession(id = "a", status = ExamStatus.Finished, grade = grade("IM2", "IH")))

        val viewModel = loadedViewModel()

        assertEquals("IM2~IH", viewModel.uiState.value.pastSessions?.single()?.gradeRange)
    }

    @Test
    fun `등급 범위의 위아래가 같으면 등급 하나만 보인다`() = runTest(dispatcher) {
        sessions.value = listOf(ExamSession(id = "a", status = ExamStatus.Finished, grade = grade("IH", "IH")))

        val viewModel = loadedViewModel()

        assertEquals("IH", viewModel.uiState.value.pastSessions?.single()?.gradeRange)
    }

    @Test
    fun `F2_6 시간이 다 돼 끝난 회차는 시간 부족으로 표시한다`() = runTest(dispatcher) {
        sessions.value = listOf(ExamSession(id = "a", status = ExamStatus.TimeUp))

        val viewModel = loadedViewModel()

        assertTrue(viewModel.uiState.value.pastSessions!!.single().timeUp)
    }

    @Test
    fun `저장소에 회차가 새로 저장되면 지난 회차에 바로 더해진다`() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        sessions.value = listOf(ExamSession(id = "a", status = ExamStatus.Finished))
        advanceUntilIdle()

        assertEquals(listOf("a"), viewModel.uiState.value.pastSessions?.map { it.id })
    }

    @Test
    fun `새로 시작하면 진행 중인 회차를 미완료로 끝내고 끝난 회차는 그대로 둔다`() = runTest(dispatcher) {
        sessions.value = listOf(ExamSession(id = "a", status = ExamStatus.Finished), ExamSession(id = "b", answerIds = listOf("x"), status = ExamStatus.InProgress))
        val viewModel = loadedViewModel()

        viewModel.onIntent(HomeIntent.StartNewExam)
        advanceUntilIdle()

        assertEquals(listOf(ExamStatus.Finished, ExamStatus.Abandoned), sessions.value.map { it.status })
        assertEquals(listOf("x"), sessions.value[1].answerIds)
        assertFalse(viewModel.uiState.value.canResume)
    }

    @Test
    fun `미완료로 끝난 회차는 다시 보기에 올리지 않는다`() = runTest(dispatcher) {
        sessions.value = listOf(ExamSession(id = "a", status = ExamStatus.Finished), ExamSession(id = "b", status = ExamStatus.Abandoned))

        val viewModel = loadedViewModel()

        assertEquals(listOf("a"), viewModel.uiState.value.pastSessions?.map { it.id })
    }

    private fun grade(low: String, high: String) = SessionGrade(checks = listOf(GradeCheck("1", true)), low = low, high = high, reasons = emptyList(), brokeAt = emptyList())
}

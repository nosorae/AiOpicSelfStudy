package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.Recalibration
import io.github.nosorae.aiopic.libs.timer.api.MonotonicTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ExamViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeExamRepository()
    private val tts = FakeTts(readTime = 3.seconds)
    private val recorder = FakeRecorder()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(content: ExamContent = testContent): ExamViewModel {
        val apply = ApplyExamEventUseCase(repository, content)
        return ExamViewModel(
            sessionId = "s1",
            loadExamUseCase = LoadExamUseCase(repository, content),
            applyExamEventUseCase = apply,
            playQuestionUseCase = PlayQuestionUseCase(repository, tts, recorder),
            submitAnswerUseCase = SubmitAnswerUseCase(repository, recorder, apply),
            stopRecordingUseCase = StopRecordingUseCase(recorder),
            timer = MonotonicTimer { dispatcher.scheduler.currentTime.milliseconds },
        )
    }

    /** 1번 문항(단계 3)에 들어와 있는 회차를 저장해 둔다. 40분 타이머가 이미 돈다. */
    private fun atFirstQuestion(replayUsed: Boolean = false) {
        repository.sessions["s1"] = ExamSession(
            id = "s1",
            stepIndex = 3,
            surveyTopics = listOf("영화", "공원"),
            level = 3,
            remaining = 40.minutes,
            replayUsed = replayUsed,
        )
    }

    /** Play 를 누르고 질문을 다 읽어 녹음이 시작될 때까지 돌린다. 이 시점이 녹음 0초다. */
    private fun TestScope.playUntilRecording(viewModel: ExamViewModel) {
        runCurrent()
        viewModel.onIntent(ExamIntent.PlayQuestion)
        advanceTimeBy(3.seconds)
        runCurrent()
    }

    private fun TestScope.advance(duration: Duration) {
        advanceTimeBy(duration)
        runCurrent()
    }

    @Test
    fun `F2_7 답변 타이머는 기본으로 꺼져 있고 설정에서 켰으면 문항 화면에 보인다`() = runTest(dispatcher) {
        atFirstQuestion()
        val off = viewModel()
        runCurrent()
        assertFalse(off.uiState.value.answerTimer)

        val on = viewModel(testContent.copy(answerTimer = true))
        playUntilRecording(on)
        advance(4.seconds)

        assertTrue(on.uiState.value.answerTimer)
        assertEquals(4.seconds, on.uiState.value.recorded)
    }

    @Test
    fun `새 회차는 서베이에서 시작하고 저장된다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertIs<ExamStep.Survey>(viewModel.uiState.value.step)
        assertEquals(0, repository.sessions.getValue("s1").stepIndex)
    }

    @Test
    fun `F2_2 서베이는 지난 회차의 선택을 미리 채운다`() = runTest(dispatcher) {
        repository.lastTopics = listOf("영화", "요리", "없어진 주제")

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(setOf("영화", "요리"), viewModel.uiState.value.selectedTopics)
    }

    @Test
    fun `F2_2 서베이에서 최소 개수보다 적게 고르면 다음으로 못 간다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(ExamIntent.ToggleTopic("영화"))
        viewModel.onIntent(ExamIntent.SubmitSurvey)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canSubmitSurvey)
        assertIs<ExamStep.Survey>(viewModel.uiState.value.step)
    }

    @Test
    fun `F2_2 서베이는 주제 앞의 Part 질문을 보이고 Part 마다 하나를 고른다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(ExamIntent.SelectSurveyAnswer(part = 0, option = 1))
        viewModel.onIntent(ExamIntent.SelectSurveyAnswer(part = 1, option = 0))
        viewModel.onIntent(ExamIntent.SelectSurveyAnswer(part = 0, option = 0))

        assertEquals(testContent.surveyParts, viewModel.uiState.value.surveyParts)
        assertEquals(mapOf(0 to 0, 1 to 0), viewModel.uiState.value.surveyAnswers)
    }

    /** Part 질문은 고르지 않았다. 다음으로 가는 조건은 Part 4 주제 수 하나다(F2.2). */
    @Test
    fun `F2_1 서베이를 내면 수준 선택으로 가고 고른 주제가 저장된다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(ExamIntent.ToggleTopic("영화"))
        viewModel.onIntent(ExamIntent.ToggleTopic("공원"))
        viewModel.onIntent(ExamIntent.SubmitSurvey)
        advanceUntilIdle()

        assertEquals(ExamStep.LevelSelect, viewModel.uiState.value.step)
        assertEquals(listOf("영화", "공원"), repository.sessions.getValue("s1").surveyTopics)
    }

    @Test
    fun `다음을 누르면 저장이 끝날 때까지 처리 중이고 그동안 다른 입력을 받지 않는다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.saveTime = 1.seconds
        viewModel.onIntent(ExamIntent.ToggleTopic("영화"))
        viewModel.onIntent(ExamIntent.ToggleTopic("공원"))

        viewModel.onIntent(ExamIntent.SubmitSurvey)
        runCurrent()
        viewModel.onIntent(ExamIntent.ToggleTopic("요리"))
        viewModel.onIntent(ExamIntent.SubmitSurvey)

        assertTrue(viewModel.uiState.value.saving)
        assertEquals(setOf("영화", "공원"), viewModel.uiState.value.selectedTopics)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.saving)
        assertEquals(ExamStep.LevelSelect, viewModel.uiState.value.step)
        assertEquals(listOf("영화", "공원"), repository.sessions.getValue("s1").surveyTopics)
    }

    @Test
    fun `F2_1 수준을 고르고 다음을 누르면 점검으로 간다`() = runTest(dispatcher) {
        repository.sessions["s1"] = ExamSession(id = "s1", stepIndex = 1, surveyTopics = listOf("영화", "공원"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(ExamIntent.SelectLevel(2))
        viewModel.onIntent(ExamIntent.SubmitLevel)
        advanceUntilIdle()

        assertEquals(ExamStep.Check, viewModel.uiState.value.step)
        assertEquals(2, repository.sessions.getValue("s1").level)
    }

    @Test
    fun `F2_1 재조정에서 고르고 다음을 누르면 고른 값이 저장된다`() = runTest(dispatcher) {
        repository.sessions["s1"] = ExamSession(id = "s1", stepIndex = 4, level = 3, remaining = 30.minutes)
        val viewModel = viewModel()
        runCurrent()

        viewModel.onIntent(ExamIntent.SelectRecalibration(Recalibration.Harder))
        viewModel.onIntent(ExamIntent.SubmitRecalibration)
        runCurrent()

        assertEquals(Recalibration.Harder, repository.sessions.getValue("s1").recalibration)
        assertIs<ExamStep.Ask>(viewModel.uiState.value.step)
    }

    @Test
    fun `F2_3 Play 를 누르면 질문을 읽고 다 읽은 뒤에 녹음이 시작된다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        runCurrent()

        viewModel.onIntent(ExamIntent.PlayQuestion)
        runCurrent()
        assertEquals(QuestionPhase.Speaking, viewModel.uiState.value.phase)
        assertNull(recorder.recording)
        advance(3.seconds)

        assertEquals(QuestionPhase.Recording, viewModel.uiState.value.phase)
        assertEquals(listOf("질문 3"), tts.spoken)
    }

    @Test
    fun `F2_4 다시 듣기는 녹음 시작 5초 전까지만 켜진다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        advance(4.seconds)
        assertTrue(viewModel.uiState.value.canReplay)
        advance(1.seconds)

        assertFalse(viewModel.uiState.value.canReplay)
    }

    @Test
    fun `F2_4 5초가 지난 뒤 다시 듣기를 보내도 질문을 다시 읽지 않는다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        advance(5.seconds)
        viewModel.onIntent(ExamIntent.ReplayQuestion)
        advance(3.seconds)

        assertEquals(1, tts.spoken.size)
    }

    @Test
    fun `F2_4 다시 듣기는 한 번만 되고 쓴 것이 저장된다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        viewModel.onIntent(ExamIntent.ReplayQuestion)
        advance(3.seconds)

        assertEquals(2, tts.spoken.size)
        assertEquals(QuestionPhase.Recording, viewModel.uiState.value.phase)
        assertFalse(viewModel.uiState.value.canReplay)
        assertTrue(repository.sessions.getValue("s1").replayUsed)
    }

    @Test
    fun `F2_4 Next 는 녹음 시작 10초 뒤에 켜진다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        advance(9.seconds)
        assertFalse(viewModel.uiState.value.canNext)
        advance(1.seconds)

        assertTrue(viewModel.uiState.value.canNext)
    }

    @Test
    fun `F2_4 10초 전에 Next 를 보내도 다음 문항으로 가지 않는다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        advance(9.seconds)
        viewModel.onIntent(ExamIntent.SubmitAnswer)
        runCurrent()

        assertEquals(3, repository.sessions.getValue("s1").stepIndex)
        assertEquals(QuestionPhase.Recording, viewModel.uiState.value.phase)
    }

    @Test
    fun `다시 들으면 Next 는 새로 시작한 녹음에서 10초 뒤에 켜진다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        advance(2.seconds)
        viewModel.onIntent(ExamIntent.ReplayQuestion)
        advance(3.seconds + 9.seconds)

        assertFalse(viewModel.uiState.value.canNext)
    }

    @Test
    fun `문항에서 Next 를 누르면 녹음 조각과 함께 답변을 저장하고 다음 단계로 간다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        advance(10.seconds)
        viewModel.onIntent(ExamIntent.SubmitAnswer)
        runCurrent()

        val answer = repository.answers.single()
        assertEquals(listOf("rec/s1/3/0.m4a"), answer.chunkPaths)
        assertEquals(listOf(answer.id), repository.sessions.getValue("s1").answerIds)
        assertEquals(ExamStep.Recalibrate, viewModel.uiState.value.step)
        assertNull(recorder.recording)
    }

    @Test
    fun `Next 를 누르면 답변 저장이 끝날 때까지 처리 중이다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        playUntilRecording(viewModel)
        advance(10.seconds)
        repository.saveTime = 500.milliseconds

        viewModel.onIntent(ExamIntent.SubmitAnswer)
        runCurrent()

        assertTrue(viewModel.uiState.value.saving)
        assertFalse(viewModel.uiState.value.canReplay)
        advance(1.seconds)
        assertFalse(viewModel.uiState.value.saving)
        assertEquals(ExamStep.Recalibrate, viewModel.uiState.value.step)
    }

    @Test
    fun `점검의 연습 문항은 답변으로 저장하지 않는다`() = runTest(dispatcher) {
        repository.sessions["s1"] = ExamSession(id = "s1", stepIndex = 2, level = 3)
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        advance(10.seconds)
        viewModel.onIntent(ExamIntent.SubmitAnswer)
        runCurrent()

        assertTrue(repository.answers.isEmpty())
        assertIs<ExamStep.Ask>(viewModel.uiState.value.step)
    }

    @Test
    fun `F2_5 40분 타이머는 문항 화면에 있는 동안 단조 시계만큼 줄어든다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        runCurrent()

        advance(30.seconds)

        assertEquals(40.minutes - 30.seconds, viewModel.uiState.value.remaining)
        assertEquals(40.minutes - 30.seconds, repository.sessions.getValue("s1").remaining)
    }

    @Test
    fun `F2_6 시간이 다 되면 녹음을 멈추고 시험 끝으로 간다`() = runTest(dispatcher) {
        repository.sessions["s1"] = ExamSession(id = "s1", stepIndex = 3, level = 3, remaining = 20.seconds)
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        advance(17.seconds)

        assertEquals(ExamStatus.TimeUp, repository.sessions.getValue("s1").status)
        assertEquals(ExamDestination.Finished, viewModel.uiState.value.navigation)
        assertNull(recorder.recording)
    }

    @Test
    fun `마지막 문항에서 Next 를 누르면 시험 끝으로 간다`() = runTest(dispatcher) {
        repository.sessions["s1"] = ExamSession(id = "s1", stepIndex = 6, level = 3, remaining = 10.minutes)
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        advance(10.seconds)
        viewModel.onIntent(ExamIntent.SubmitAnswer)
        runCurrent()

        assertEquals(ExamStatus.Finished, repository.sessions.getValue("s1").status)
        assertEquals(ExamDestination.Finished, viewModel.uiState.value.navigation)
    }

    @Test
    fun `F3_3 다시 켜면 같은 문항에서 이어 녹음하고 다시 듣기를 안 썼으면 질문을 한 번 더 듣는다`() = runTest(dispatcher) {
        atFirstQuestion()
        recorder.start("rec/s1/3")
        recorder.stop()
        val viewModel = viewModel()

        playUntilRecording(viewModel)

        assertEquals(listOf("질문 3"), tts.spoken)
        assertEquals("rec/s1/3", recorder.recording)
        assertTrue(repository.sessions.getValue("s1").replayUsed)
    }

    @Test
    fun `F3_3 다시 듣기를 이미 썼으면 다시 켠 뒤 질문 없이 바로 이어 녹음한다`() = runTest(dispatcher) {
        atFirstQuestion(replayUsed = true)
        recorder.start("rec/s1/3")
        recorder.stop()
        val viewModel = viewModel()
        runCurrent()

        viewModel.onIntent(ExamIntent.PlayQuestion)
        runCurrent()

        assertTrue(tts.spoken.isEmpty())
        assertEquals(QuestionPhase.Recording, viewModel.uiState.value.phase)
    }

    @Test
    fun `마이크를 열지 못하면 녹음하지 않고 이유를 보여 준다`() = runTest(dispatcher) {
        atFirstQuestion()
        recorder.micAvailable = false
        val viewModel = viewModel()

        playUntilRecording(viewModel)

        assertEquals(ExamError.MicUnavailable, viewModel.uiState.value.error)
        assertEquals(QuestionPhase.Ready, viewModel.uiState.value.phase)
    }

    @Test
    fun `마이크 권한을 거절하면 이유를 보여 준다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        runCurrent()

        viewModel.onIntent(ExamIntent.MicDenied)

        assertEquals(ExamError.MicUnavailable, viewModel.uiState.value.error)
    }

    @Test
    fun `질문을 읽지 못하면 녹음하지 않고 이유를 보여 준다`() = runTest(dispatcher) {
        atFirstQuestion()
        tts.canSpeak = false
        val viewModel = viewModel()

        playUntilRecording(viewModel)

        assertEquals(ExamError.SpeechFailed, viewModel.uiState.value.error)
        assertNull(recorder.recording)
    }

    @Test
    fun `나가기를 누르면 녹음을 멈추고 회차를 남긴 채 나간다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        viewModel.onIntent(ExamIntent.ExitExam)
        runCurrent()

        assertEquals(ExamDestination.Exit, viewModel.uiState.value.navigation)
        assertNull(recorder.recording)
        assertEquals(3, repository.sessions.getValue("s1").stepIndex)
    }

    @Test
    fun `문항 번호는 고른 수준에서 내는 문항만 센다`() = runTest(dispatcher) {
        repository.sessions["s1"] = ExamSession(id = "s1", stepIndex = 5, level = 1, remaining = 10.minutes)
        val viewModel = viewModel()
        runCurrent()

        assertEquals(2, viewModel.uiState.value.questionNumber)
        assertEquals(2, viewModel.uiState.value.questionCount)
    }
}

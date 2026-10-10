package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.question.model.SurveyAnswers
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.Recalibration
import io.github.nosorae.aiopic.libs.timer.api.MonotonicTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val surveyStore = FakeSurveyStore()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(content: ExamContent = testContent): ExamViewModel {
        val apply = ApplyExamEventUseCase(repository, content)
        return ExamViewModel(
            sessionId = "s1",
            loadExamUseCase = LoadExamUseCase(repository, surveyStore, content),
            applyExamEventUseCase = apply,
            playQuestionUseCase = PlayQuestionUseCase(repository, tts, recorder),
            submitAnswerUseCase = SubmitAnswerUseCase(repository, recorder, apply),
            stopRecordingUseCase = StopRecordingUseCase(recorder),
            observeVoiceLevelUseCase = ObserveVoiceLevelUseCase(recorder),
            submitSurveyUseCase = SubmitSurveyUseCase(SaveSurveyAnswersUseCase(surveyStore), apply),
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
    fun `F2_2 서베이는 저장한 답으로 미리 채우고 없어진 주제는 뺀다`() = runTest(dispatcher) {
        surveyStore.saved = SurveyAnswers(mapOf("work" to "student"), listOf("영화", "요리", "없어진 주제"))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(setOf("영화", "요리"), viewModel.uiState.value.survey?.topics)
        assertEquals(mapOf("work" to "student"), viewModel.uiState.value.survey?.choices)
    }

    @Test
    fun `F2_2 서베이에서 묶음 최소나 전체 최소를 못 채우면 다음으로 못 간다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        answerQuestions(viewModel)

        viewModel.onIntent(ExamIntent.ToggleTopic("영화"))
        viewModel.onIntent(ExamIntent.ToggleTopic("공원"))
        viewModel.onIntent(ExamIntent.SubmitSurvey)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canSubmitSurvey, "취미 묶음이 0개다")
        assertIs<ExamStep.Survey>(viewModel.uiState.value.step)
    }

    @Test
    fun `F2_2 서베이는 한 화면에 질문 하나이고 고르면 바로 다음 화면이며 하위 질문은 이어지는 화면으로 나온다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(ExamIntent.ChooseSurveyOption("office"))
        assertEquals("period", viewModel.uiState.value.survey?.question?.id)
        viewModel.onIntent(ExamIntent.PreviousSurveyScreen)
        viewModel.onIntent(ExamIntent.ChooseSurveyOption("student"))

        assertEquals("home", viewModel.uiState.value.survey?.question?.id)
    }

    @Test
    fun `F2_1 서베이를 내면 수준 선택으로 가고 고른 주제가 회차에 저장된다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        answerQuestions(viewModel)

        viewModel.onIntent(ExamIntent.ToggleTopic("영화"))
        viewModel.onIntent(ExamIntent.NextSurveyScreen)
        viewModel.onIntent(ExamIntent.ToggleTopic("요리"))
        viewModel.onIntent(ExamIntent.SubmitSurvey)
        advanceUntilIdle()

        assertEquals(ExamStep.LevelSelect, viewModel.uiState.value.step)
        assertEquals(listOf("영화", "요리"), repository.sessions.getValue("s1").surveyTopics)
    }

    @Test
    fun `서베이를 내면 답이 저장돼 다음 서베이와 설정이 같은 값을 읽는다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        answerQuestions(viewModel)

        viewModel.onIntent(ExamIntent.ToggleTopic("영화"))
        viewModel.onIntent(ExamIntent.ToggleTopic("요리"))
        viewModel.onIntent(ExamIntent.SubmitSurvey)
        advanceUntilIdle()

        assertEquals(SurveyAnswers(mapOf("work" to "student", "home" to "alone"), listOf("영화", "요리")), surveyStore.saved)
    }

    @Test
    fun `다음을 누르면 저장이 끝날 때까지 처리 중이고 그동안 다른 입력을 받지 않는다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        answerQuestions(viewModel)
        repository.saveTime = 1.seconds
        viewModel.onIntent(ExamIntent.ToggleTopic("영화"))
        viewModel.onIntent(ExamIntent.ToggleTopic("요리"))

        viewModel.onIntent(ExamIntent.SubmitSurvey)
        runCurrent()
        viewModel.onIntent(ExamIntent.ToggleTopic("공원"))
        viewModel.onIntent(ExamIntent.SubmitSurvey)

        assertTrue(viewModel.uiState.value.saving)
        assertEquals(setOf("영화", "요리"), viewModel.uiState.value.survey?.topics)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.saving)
        assertEquals(ExamStep.LevelSelect, viewModel.uiState.value.step)
        assertEquals(listOf("영화", "요리"), repository.sessions.getValue("s1").surveyTopics)
    }

    /** 하나만 고르는 질문(일: 학생, 사는 곳: 혼자)을 지나 첫 묶음 화면에 선다. */
    private fun answerQuestions(viewModel: ExamViewModel) {
        viewModel.onIntent(ExamIntent.ChooseSurveyOption("student"))
        viewModel.onIntent(ExamIntent.ChooseSurveyOption("alone"))
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
    fun `녹음하는 동안만 마이크 음량을 목소리 띠에 넘기고 녹음이 끝나면 0 으로 돌린다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        recorder.level.value = 0.9f
        runCurrent()
        assertEquals(0f, viewModel.uiState.value.voiceLevel)
        assertEquals(40.minutes, viewModel.uiState.value.timeLimit)

        playUntilRecording(viewModel)
        recorder.level.value = 0.6f
        runCurrent()
        assertEquals(0.6f, viewModel.uiState.value.voiceLevel)

        advance(NextDelay)
        viewModel.onIntent(ExamIntent.SubmitAnswer)
        runCurrent()
        recorder.level.value = 0.8f
        runCurrent()

        assertEquals(0f, viewModel.uiState.value.voiceLevel)
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
    fun `F6_1 질문이 바로 오면 불러오는 중 안내 없이 질문을 읽는다`() = runTest(dispatcher) {
        atFirstQuestion()
        val viewModel = viewModel()
        val notices = mutableListOf<QuestionNotice?>()
        backgroundScope.launch { viewModel.uiState.collect { notices += it.questionNotice } }

        playUntilRecording(viewModel)

        assertEquals(QuestionPhase.Recording, viewModel.uiState.value.phase)
        assertEquals(listOf<QuestionNotice?>(null), notices.distinct())
    }

    @Test
    fun `F6_1 질문을 기다리면 무엇을 만드는지 보이고 10초가 넘으면 오래 걸린다는 안내로 바뀌며 질문이 오면 지운다`() = runTest(dispatcher) {
        atFirstQuestion()
        repository.questionTime = 12.seconds
        val viewModel = viewModel()
        runCurrent()

        viewModel.onIntent(ExamIntent.PlayQuestion)
        advance(1.seconds)
        assertEquals(QuestionNotice.Making(1, 1, recalibrated = false), viewModel.uiState.value.questionNotice, "1번부터 재조정 앞까지 서베이 주제로")
        advance(9.seconds)
        assertEquals(QuestionNotice.Slow, viewModel.uiState.value.questionNotice)
        advance(2.seconds)
        assertNull(viewModel.uiState.value.questionNotice)
        assertEquals(QuestionPhase.Speaking, viewModel.uiState.value.phase)
    }

    @Test
    fun `F6_1 재조정 뒤 문항에서 기다리면 고른 난이도로 끝까지 만든다고 보인다`() = runTest(dispatcher) {
        repository.sessions["s1"] = ExamSession("s1", stepIndex = 5, surveyTopics = listOf("영화", "공원"), level = 3, recalibration = Recalibration.Harder, remaining = 30.minutes)
        repository.questionTime = 2.seconds
        val viewModel = viewModel()
        runCurrent()

        viewModel.onIntent(ExamIntent.PlayQuestion)
        advance(1.seconds)

        assertEquals(QuestionNotice.Making(2, 3, recalibrated = true), viewModel.uiState.value.questionNotice)
    }

    @Test
    fun `F6_2 모델 출제가 실패해 내장 문항으로 바꾸면 그 문항 동안 알리고 다음 단계에서 지운다`() = runTest(dispatcher) {
        atFirstQuestion()
        repository.fellBack = true
        val viewModel = viewModel()

        playUntilRecording(viewModel)
        assertEquals(QuestionNotice.BuiltIn, viewModel.uiState.value.questionNotice)
        advance(11.seconds)
        viewModel.onIntent(ExamIntent.SubmitAnswer)
        runCurrent()

        assertNull(viewModel.uiState.value.questionNotice)
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
    fun `마지막 문항까지 답하고 끝나면 다 답했다고 표시해 끝 축하를 보이게 한다`() = runTest(dispatcher) {
        repository.sessions["s1"] = ExamSession(id = "s1", stepIndex = 6, level = 3, remaining = 10.minutes)
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        advance(10.seconds)
        viewModel.onIntent(ExamIntent.SubmitAnswer)
        runCurrent()

        assertTrue(viewModel.uiState.value.answeredAll)
    }

    @Test
    fun `시간이 다 돼 끝나면 다 답했다고 표시하지 않는다`() = runTest(dispatcher) {
        repository.sessions["s1"] = ExamSession(id = "s1", stepIndex = 3, level = 3, remaining = 20.seconds)
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        advance(17.seconds)

        assertFalse(viewModel.uiState.value.answeredAll)
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

        assertEquals(ExamError.MicDenied, viewModel.uiState.value.error)
    }

    @Test
    fun `녹음 조각을 쓰지 못했으면 Next 에서 답변을 저장하지 않고 이유를 보여 준다`() = runTest(dispatcher) {
        atFirstQuestion()
        recorder.writeFails = true
        val viewModel = viewModel()
        playUntilRecording(viewModel)
        advance(NextDelay)

        viewModel.onIntent(ExamIntent.SubmitAnswer)
        runCurrent()

        assertEquals(ExamError.RecordingFailed, viewModel.uiState.value.error)
        assertEquals(QuestionPhase.Ready, viewModel.uiState.value.phase)
        assertTrue(repository.answers.isEmpty())
        assertEquals(3, repository.sessions.getValue("s1").stepIndex)
    }

    @Test
    fun `녹음 조각을 쓰지 못한 뒤 다시 녹음하면 같은 문항에 이어 녹음한다`() = runTest(dispatcher) {
        atFirstQuestion()
        recorder.writeFails = true
        val viewModel = viewModel()
        playUntilRecording(viewModel)
        advance(NextDelay)
        viewModel.onIntent(ExamIntent.SubmitAnswer)
        runCurrent()
        recorder.writeFails = false

        playUntilRecording(viewModel)

        assertNull(viewModel.uiState.value.error)
        assertEquals(QuestionPhase.Recording, viewModel.uiState.value.phase)
        assertEquals("rec/s1/3", recorder.recording)
    }

    @Test
    fun `다시 듣기 전에 녹음 조각을 쓰지 못했으면 이어 녹음하지 않고 이유를 보여 준다`() = runTest(dispatcher) {
        atFirstQuestion()
        recorder.writeFails = true
        val viewModel = viewModel()
        playUntilRecording(viewModel)

        viewModel.onIntent(ExamIntent.ReplayQuestion)
        runCurrent()

        assertEquals(ExamError.RecordingFailed, viewModel.uiState.value.error)
        assertEquals(QuestionPhase.Ready, viewModel.uiState.value.phase)
        assertNull(recorder.recording)
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

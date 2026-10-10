package io.github.nosorae.aiopic.ui.featureexam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.nosorae.aiopic.engine.session.model.ExamEvent
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.libs.timer.api.MonotonicTimer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 모의시험 화면(S4~S8). 서베이, 수준 선택, 점검, 문항, 재조정을 회차의 지금 단계에 따라 그린다.
 * [sessionId] 는 화면 인자다. 홈에서 새 id 로 들어오면 새 회차, 저장된 id 로 들어오면 이어 보기다(F3).
 *
 * 시간은 단조 시계 [timer] 로 잰다. 이 ViewModel 이 살아 있는 동안만 1초마다 흐른 시간을 회차에 넣으므로
 * 나가거나 앱이 꺼진 동안 40분 타이머가 멈추고, 기기 시각을 바꿔도 흔들리지 않는다(F2.5).
 */
@AssistedInject
class ExamViewModel(
    @Assisted private val sessionId: String,
    private val loadExamUseCase: LoadExamUseCase,
    private val applyExamEventUseCase: ApplyExamEventUseCase,
    private val playQuestionUseCase: PlayQuestionUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase,
    private val observeVoiceLevelUseCase: ObserveVoiceLevelUseCase,
    private val submitSurveyUseCase: SubmitSurveyUseCase,
    private val timer: MonotonicTimer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private lateinit var content: ExamContent
    private lateinit var session: ExamSession

    /** 지금 녹음이 시작된 단조 시계 값. 녹음 중이 아니면 null 이다. */
    private var recordingStartedAt: Duration? = null
    private var lastTick = Duration.ZERO
    private var clock: Job? = null
    private var playing: Job? = null

    /** 녹음하는 동안만 마이크 음량을 모은다. */
    private var listening: Job? = null

    /** 시계와 버튼이 같은 회차에서 각자 다음 회차를 계산해 한쪽 저장을 덮어쓰지 않게 한 번에 하나씩 바꾼다. */
    private val sessionLock = Mutex()

    init {
        viewModelScope.launch {
            val loaded = loadExamUseCase(sessionId)
            content = loaded.content
            _uiState.update {
                it.copy(
                    survey = loaded.survey,
                    levelCount = content.spec.levelCount,
                    timeLimit = content.spec.timeLimit,
                    answerTimer = content.answerTimer,
                )
            }
            changed(loaded.session)
        }
    }

    fun onIntent(intent: ExamIntent) {
        val state = _uiState.value
        if (state.step == null || (state.saving && intent != ExamIntent.NavigationHandled)) return
        when (intent) {
            is ExamIntent.ChooseSurveyOption -> updateSurvey { it.choose(intent.option) }

            is ExamIntent.ToggleTopic -> updateSurvey { it.toggle(intent.topic) }

            ExamIntent.NextSurveyScreen -> updateSurvey { it.next() }

            ExamIntent.PreviousSurveyScreen -> updateSurvey { it.back() }

            ExamIntent.SubmitSurvey -> state.survey?.takeIf { state.canSubmitSurvey }?.answers()?.let { answers ->
                save { commit { submitSurveyUseCase(it, answers) } }
            }

            is ExamIntent.SelectLevel -> _uiState.update { it.copy(selectedLevel = intent.level) }

            ExamIntent.SubmitLevel -> _uiState.value.selectedLevel?.let { apply(ExamEvent.LevelSelected(it)) }

            is ExamIntent.SelectRecalibration -> _uiState.update { it.copy(selectedRecalibration = intent.choice) }

            ExamIntent.SubmitRecalibration -> _uiState.value.selectedRecalibration?.let { apply(ExamEvent.Recalibrated(it)) }

            ExamIntent.PlayQuestion -> play()

            ExamIntent.ReplayQuestion -> replay()

            ExamIntent.SubmitAnswer -> submit()

            ExamIntent.MicDenied -> _uiState.update { it.copy(phase = QuestionPhase.Ready, error = ExamError.MicDenied) }

            ExamIntent.ExitExam -> exit()

            ExamIntent.NavigationHandled -> _uiState.update { it.copy(navigation = null) }
        }
    }

    private fun updateSurvey(change: (SurveyState) -> SurveyState) = _uiState.update { state -> state.copy(survey = state.survey?.let(change)) }

    private fun apply(event: ExamEvent) = save { commit { applyExamEventUseCase(it, event) } }

    /** 사용자가 누른 저장을 하는 동안 [ExamUiState.saving] 을 켜 둔다. 1초마다 하는 시간 저장은 여기로 오지 않는다. */
    private fun save(block: suspend () -> Unit) {
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _uiState.update { it.copy(saving = false) }
            }
        }
    }

    private fun play() {
        val state = _uiState.value
        if (state.phase != QuestionPhase.Ready || (state.step !is ExamStep.Ask && state.step !is ExamStep.Check)) return
        _uiState.update { it.copy(phase = QuestionPhase.Speaking, error = null) }
        playing = viewModelScope.launch { recordingStarted(playQuestionUseCase(session, making(), ::showNotice)) }
    }

    /** 녹음 시작 5초 안에 한 번만 된다(F2.4). 녹음을 멈추고 질문을 다시 들은 뒤 같은 폴더에 이어 녹음한다. */
    private fun replay() {
        val startedAt = recordingStartedAt ?: return
        if (session.replayUsed || timer.elapsed() - startedAt >= ReplayWindow) return
        recordingEnded()
        _uiState.update { it.copy(phase = QuestionPhase.Speaking) }
        playing = viewModelScope.launch {
            val failed = stopRecordingUseCase()
            recordingStarted(failed?.let(PlayResult::Failed) ?: playQuestionUseCase(session, making(), ::showNotice))
        }
    }

    private fun showNotice(notice: QuestionNotice?) = _uiState.update { it.copy(questionNotice = notice) }

    /** 지금 문항부터 다음 재조정(없으면 끝) 앞까지가 한 번에 만드는 질문이다. 재조정을 지났으면 고른 난이도에 맞춘다. */
    private fun making(): QuestionNotice.Making {
        val steps = content.spec.steps
        val until = (session.stepIndex until steps.size).firstOrNull { steps[it] == ExamStep.Recalibrate } ?: steps.size
        val asks = steps.indices.filter { i -> (steps[i] as? ExamStep.Ask)?.let { it.minLevel <= (session.level ?: 1) } == true }
        val first = _uiState.value.questionNumber.coerceAtLeast(1)
        return QuestionNotice.Making(first, asks.count { it < until }.coerceAtLeast(first), session.recalibration != null)
    }

    private suspend fun recordingStarted(result: PlayResult) {
        when (result) {
            is PlayResult.Failed -> _uiState.update { it.copy(phase = QuestionPhase.Ready, error = result.error) }

            is PlayResult.Recording -> {
                // 이어 녹음하며 질문을 다시 들었으면 그 문항의 다시 듣기를 쓴 것이다(F3.3).
                if (result.resumed) commit { if (it.replayUsed) it else applyExamEventUseCase(it, ExamEvent.Replay) }
                recordingStartedAt = timer.elapsed()
                _uiState.update { it.copy(phase = QuestionPhase.Recording, recorded = Duration.ZERO) }
                listening = viewModelScope.launch { observeVoiceLevelUseCase().collect { level -> _uiState.update { it.copy(voiceLevel = level) } } }
                // 시계를 녹음 시작에 맞춰 다시 돌려 5초, 10초 경계에서 버튼이 바로 바뀌게 한다.
                startClock(restart = true)
            }
        }
    }

    /** Next 는 녹음 시작 10초 뒤에만 된다(F2.4). 지나간 문항으로 돌아가는 길은 없다. */
    private fun submit() {
        val startedAt = recordingStartedAt ?: return
        if (timer.elapsed() - startedAt < NextDelay) return
        recordingEnded()
        val scored = _uiState.value.step is ExamStep.Ask
        save {
            sessionLock.withLock {
                when (val result = submitAnswerUseCase(session, scored)) {
                    is SubmitResult.Moved -> changed(result.session)

                    // 같은 문항에 남는다. Play 를 다시 누르면 남은 조각 뒤에 이어 녹음한다(F3.3).
                    is SubmitResult.Failed -> _uiState.update { it.copy(phase = QuestionPhase.Ready, error = result.error) }
                }
            }
        }
    }

    private fun exit() = save {
        stopAll()
        _uiState.update { it.copy(navigation = ExamDestination.Exit) }
    }

    /** 시계가 시간 끝을 알린 경우 시계 코루틴 안에서 불리므로 녹음을 먼저 멈추고 시계를 마지막에 멈춘다. */
    private suspend fun stopAll() {
        playing?.cancel()
        if (recordingStartedAt != null) {
            recordingEnded()
            stopRecordingUseCase()
        }
        clock?.cancel()
    }

    /** 녹음 시작 시각과 음량 모으기를 지운다. 목소리 띠는 다음 녹음까지 가라앉는다. */
    private fun recordingEnded() {
        recordingStartedAt = null
        listening?.cancel()
        _uiState.update { it.copy(voiceLevel = 0f) }
    }

    private suspend fun commit(change: suspend (ExamSession) -> ExamSession) = sessionLock.withLock { changed(change(session)) }

    /** 바뀐 회차를 화면에 옮긴다. 단계가 바뀌면 문항 진행을 처음으로 돌리고, 회차가 끝났으면 녹음을 멈추고 끝으로 간다. */
    private suspend fun changed(next: ExamSession) {
        val stepChanged = !::session.isInitialized || session.stepIndex != next.stepIndex
        // 점검 녹음 때부터 돌던 시계가 1번 문항 전의 시간을 40분에 넣지 않게 40분이 켜지는 때부터 센다(F2.5).
        if (::session.isInitialized && session.remaining == null && next.remaining != null) lastTick = timer.elapsed()
        session = next
        val asks = content.spec.steps.indices.filter { i ->
            val step = content.spec.steps[i]
            step is ExamStep.Ask && step.minLevel <= (next.level ?: 1)
        }
        _uiState.update {
            it.copy(
                step = content.spec.steps[next.stepIndex],
                questionNumber = asks.indexOf(next.stepIndex) + 1,
                questionCount = asks.size,
                remaining = next.remaining,
                replayUsed = next.replayUsed,
            ).let { state -> if (stepChanged) state.copy(phase = QuestionPhase.Ready, recorded = Duration.ZERO, error = null, questionNotice = null) else state }
        }
        if (next.status != ExamStatus.InProgress) {
            stopAll()
            _uiState.update { it.copy(navigation = ExamDestination.Finished, answeredAll = next.status == ExamStatus.Finished) }
        } else if (next.remaining != null) {
            startClock(restart = false)
        }
    }

    /** 1초마다 흐른 시간을 잰다. 이미 돌고 있으면 [restart] 일 때만 지금부터 다시 센다. 흐른 시간은 놓치지 않는다. */
    private fun startClock(restart: Boolean) {
        val running = clock?.isActive == true
        if (running && !restart) return
        clock?.cancel()
        if (!running) lastTick = timer.elapsed()
        clock = viewModelScope.launch {
            while (true) {
                delay(TICK)
                tick()
            }
        }
    }

    // ponytail: 1초마다 회차를 통째로 저장한다. 저장이 느려지면 녹음 조각이 끝날 때만 저장하게 바꾼다.
    private suspend fun tick() {
        val now = timer.elapsed()
        val passed = now - lastTick
        lastTick = now
        recordingStartedAt?.let { startedAt -> _uiState.update { it.copy(recorded = now - startedAt) } }
        commit { applyExamEventUseCase(it, ExamEvent.TimePassed(passed)) }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(sessionId: String): ExamViewModel
    }

    private companion object {
        val TICK = 1.seconds
    }
}

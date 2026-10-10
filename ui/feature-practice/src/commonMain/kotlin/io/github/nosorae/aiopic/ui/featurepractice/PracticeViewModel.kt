package io.github.nosorae.aiopic.ui.featurepractice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.nosorae.aiopic.libs.timer.api.MonotonicTimer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 한 문항 연습 화면(S12). 약한 유형에서 질문 하나를 골라 모의시험과 같은 규칙으로 답하게 한다(F4.1).
 * Next 를 누르면 답변을 저장하고 그 답변의 리포트로 간다(F4.2). [answerId] 는 화면 인자로, 홈이 들어올 때마다 새로 만든다.
 *
 * 다시 듣기와 Next 의 5초, 10초는 단조 시계 [timer] 로 잰다. 40분 타이머는 없다.
 */
// ponytail: 연습 중 앱이 꺼지면 녹음을 버리고 처음부터 다시 한다. 질문 하나라 이어 보기를 두지 않았다.
@AssistedInject
class PracticeViewModel(
    @Assisted private val answerId: String,
    private val pickPracticeQuestionUseCase: PickPracticeQuestionUseCase,
    private val playPracticeQuestionUseCase: PlayPracticeQuestionUseCase,
    private val submitPracticeAnswerUseCase: SubmitPracticeAnswerUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase,
    private val timer: MonotonicTimer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    /** 지금 녹음이 시작된 단조 시계 값. 녹음 중이 아니면 null 이다. */
    private var recordingStartedAt: Duration? = null
    private var playing: Job? = null
    private var clock: Job? = null

    init {
        viewModelScope.launch {
            val question = pickPracticeQuestionUseCase()
            _uiState.update { it.copy(question = question, error = if (question == null) PracticeError.QuestionUnavailable else null) }
        }
    }

    fun onIntent(intent: PracticeIntent) {
        if (_uiState.value.saving && intent != PracticeIntent.NavigationHandled) return
        when (intent) {
            PracticeIntent.PlayQuestion -> if (_uiState.value.canPlay) play(stopFirst = false)
            PracticeIntent.ReplayQuestion -> replay()
            PracticeIntent.SubmitAnswer -> submit()
            PracticeIntent.MicDenied -> _uiState.update { it.copy(phase = PracticePhase.Ready, error = PracticeError.MicUnavailable) }
            PracticeIntent.ExitPractice -> exit()
            PracticeIntent.NavigationHandled -> _uiState.update { it.copy(navigation = null) }
        }
    }

    private fun play(stopFirst: Boolean) {
        val question = _uiState.value.question ?: return
        recordingStartedAt = null
        clock?.cancel()
        _uiState.update { it.copy(phase = PracticePhase.Speaking, error = null) }
        playing = viewModelScope.launch {
            if (stopFirst) stopRecordingUseCase()
            val error = playPracticeQuestionUseCase(question, answerId)
            if (error != null) {
                _uiState.update { it.copy(phase = PracticePhase.Ready, error = error) }
                return@launch
            }
            val startedAt = timer.elapsed()
            recordingStartedAt = startedAt
            _uiState.update { it.copy(phase = PracticePhase.Recording, recorded = Duration.ZERO) }
            clock = launch {
                while (true) {
                    delay(TICK)
                    _uiState.update { it.copy(recorded = timer.elapsed() - startedAt) }
                }
            }
        }
    }

    /** 녹음 시작 5초 안에 한 번만 된다. 녹음을 멈추고 질문을 다시 들은 뒤 같은 폴더에 이어 녹음한다. */
    private fun replay() {
        val startedAt = recordingStartedAt ?: return
        if (_uiState.value.replayUsed || timer.elapsed() - startedAt >= ReplayWindow) return
        _uiState.update { it.copy(replayUsed = true) }
        play(stopFirst = true)
    }

    /** Next 는 녹음 시작 10초 뒤에만 된다. */
    private fun submit() {
        val startedAt = recordingStartedAt ?: return
        val question = _uiState.value.question ?: return
        if (timer.elapsed() - startedAt < NextDelay) return
        recordingStartedAt = null
        clock?.cancel()
        save {
            submitPracticeAnswerUseCase(question, answerId)
            _uiState.update { it.copy(navigation = PracticeDestination.Feedback(answerId)) }
        }
    }

    private fun exit() {
        playing?.cancel()
        clock?.cancel()
        val recording = recordingStartedAt != null
        recordingStartedAt = null
        save {
            if (recording) stopRecordingUseCase()
            _uiState.update { it.copy(navigation = PracticeDestination.Exit) }
        }
    }

    /** 사용자가 누른 저장을 하는 동안 [PracticeUiState.saving] 을 켜 둔다. */
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

    @AssistedFactory
    fun interface Factory {
        fun create(answerId: String): PracticeViewModel
    }

    private companion object {
        val TICK = 1.seconds
    }
}

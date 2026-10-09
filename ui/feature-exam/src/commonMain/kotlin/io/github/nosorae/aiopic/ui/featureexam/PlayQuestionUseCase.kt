package io.github.nosorae.aiopic.ui.featureexam

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.libs.recorder.api.Recorder
import io.github.nosorae.aiopic.libs.tts.api.Tts

/**
 * 질문을 기기 음성으로 끝까지 읽고 녹음을 시작한다(F2.3). 이 단계에 녹음 조각이 이미 있으면 이어 녹음이다.
 * 이어 녹음인데 다시 듣기를 이미 썼으면 질문을 읽지 않고 바로 녹음한다(F3.3). 다시 듣기도 이어 녹음과 같은 길로 온다.
 */
@Inject
class PlayQuestionUseCase(
    private val repository: ExamRepository,
    private val tts: Tts,
    private val recorder: Recorder,
) {
    suspend operator fun invoke(session: ExamSession): PlayResult {
        val question = repository.question(session) ?: return PlayResult.Failed(ExamError.QuestionUnavailable)
        val directory = repository.recordingDirectory(session)
        val resumed = recorder.chunks(directory).isNotEmpty()
        if (!(resumed && session.replayUsed) && !tts.speak(question.text)) return PlayResult.Failed(ExamError.SpeechFailed)
        if (!recorder.start(directory)) return PlayResult.Failed(ExamError.MicUnavailable)
        return PlayResult.Recording(resumed)
    }
}

sealed interface PlayResult {
    /** 녹음이 시작됐다. [resumed] 면 이 단계의 녹음을 이어 쓴다. */
    data class Recording(val resumed: Boolean) : PlayResult

    data class Failed(val error: ExamError) : PlayResult
}

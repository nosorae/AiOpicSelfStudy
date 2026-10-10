package io.github.nosorae.aiopic.ui.featurepractice

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.libs.recorder.api.Recorder
import io.github.nosorae.aiopic.libs.tts.api.Tts

/** 질문을 기기 음성으로 끝까지 읽고 녹음을 시작한다. 다시 듣기도 같은 길로 와서 같은 폴더에 이어 녹음한다. 성공하면 null 이다. */
@Inject
class PlayPracticeQuestionUseCase(
    private val repository: PracticeRepository,
    private val tts: Tts,
    private val recorder: Recorder,
) {
    suspend operator fun invoke(question: Question, answerId: String): PracticeError? = when {
        !tts.speak(question.text) -> PracticeError.SpeechFailed
        else -> recorder.start(repository.recordingDirectory(answerId))?.toPracticeError()
    }
}

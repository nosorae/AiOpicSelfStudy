package io.github.nosorae.aiopic.ui.featureexam

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.libs.recorder.api.Recorder

/** 녹음을 멈춘다. 쓴 조각은 남아서 같은 단계로 돌아오면 이어 쓴다(F3.3). 녹음 도중 조각을 쓰지 못했으면 그 이유를 돌려준다. */
@Inject
class StopRecordingUseCase(private val recorder: Recorder) {
    suspend operator fun invoke(): ExamError? = recorder.stop().failure?.toExamError()
}

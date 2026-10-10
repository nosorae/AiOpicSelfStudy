package io.github.nosorae.aiopic.ui.featurepractice

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.libs.recorder.api.Recorder

/** 녹음을 멈춘다. 녹음 도중 조각을 쓰지 못했으면 그 이유를 돌려준다. */
@Inject
class StopRecordingUseCase(private val recorder: Recorder) {
    suspend operator fun invoke(): PracticeError? = recorder.stop().failure?.toPracticeError()
}

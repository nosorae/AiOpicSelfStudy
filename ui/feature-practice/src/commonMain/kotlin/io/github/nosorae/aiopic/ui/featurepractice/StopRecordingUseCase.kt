package io.github.nosorae.aiopic.ui.featurepractice

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.libs.recorder.api.Recorder

/** 녹음을 멈춘다. 다시 듣기 전과 나갈 때 쓴다. */
@Inject
class StopRecordingUseCase(private val recorder: Recorder) {
    suspend operator fun invoke() {
        recorder.stop()
    }
}

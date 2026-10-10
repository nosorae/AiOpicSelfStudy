package io.github.nosorae.aiopic.ui.featureexam

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.libs.recorder.api.Recorder
import kotlinx.coroutines.flow.Flow

/** 녹음 중 마이크 음량(0~1)을 80ms 마다 낸다. 녹음 화면의 목소리 띠가 내 목소리가 들어가는 것을 보이는 데 쓴다. 모으는 동안 끝나지 않는다. */
@Inject
class ObserveVoiceLevelUseCase(private val recorder: Recorder) {
    operator fun invoke(): Flow<Float> = recorder.levels
}

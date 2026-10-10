package io.github.nosorae.aiopic.libs.recorder.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.log10
import kotlin.time.Duration.Companion.milliseconds

/** 녹음 화면의 목소리 띠가 끊겨 보이지 않을 만큼 자주, 그리면서 밀리지 않을 만큼 드물게 읽는다. */
private val LEVEL_INTERVAL = 80.milliseconds

/** 이보다 작은 소리는 0 으로 친다. 조용한 방의 잡음이 이 근처라 말하지 않을 때 띠가 가만히 있다. */
private const val FLOOR_DB = -50f

/** [readPeak] 가 돌려주는 직전 읽기 뒤 최대 진폭(0~1)을 [LEVEL_INTERVAL] 마다 [loudness] 로 바꿔 낸다. 모으는 동안 끝나지 않는다. */
internal fun levelFlow(readPeak: () -> Float): Flow<Float> = flow {
    while (true) {
        emit(loudness(readPeak()))
        delay(LEVEL_INTERVAL)
    }
}

/**
 * 최대 진폭(0~1)을 [FLOOR_DB]~0dB 구간의 0~1 로 바꾼다. 귀는 소리 크기를 로그로 듣는다.
 * 진폭 그대로 쓰면 보통 말소리가 0.1 안팎이라 띠가 거의 부풀지 않는다.
 */
internal fun loudness(peak: Float): Float {
    if (peak <= 0f) return 0f
    return ((20 * log10(peak) - FLOOR_DB) / -FLOOR_DB).coerceIn(0f, 1f)
}

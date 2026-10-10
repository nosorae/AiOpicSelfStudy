package io.github.nosorae.aiopic.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

private const val MAX_STEP_SECONDS = 0.05f

/**
 * 계속 움직이는 그림(목소리 띠, 모이는 점)의 시계. 초 단위로 흐르고 그리는 단계에서만 읽어 매 프레임 다시 구성하지 않는다.
 * 동작 줄이기면 0 에 멈춰 있다. 앱이 뒤로 가면 프레임 시계가 서서 이 값도 멈추고, 돌아오면 멈춘 곳에서 이어 흐른다.
 * 한 프레임에 0.05초보다 많이 가지 않게 막아, 돌아왔을 때 그림이 한 번에 건너뛰지 않는다.
 */
@Composable
internal fun rememberFrameSeconds(): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    if (!MontageMotion.reduceMotion) {
        LaunchedEffect(Unit) {
            var last = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    seconds.floatValue += ((now - last) / 1e9f).coerceAtMost(MAX_STEP_SECONDS)
                    last = now
                }
            }
        }
    }
    return seconds
}

package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * 녹음 시작처럼 정해진 때부터 [total] 까지 흐른 비율(0~1). Next 가 켜지기까지, 다시 듣기 창이 닫히기까지를 버튼 채움으로 보일 때 쓴다.
 * [elapsed] 는 1초마다 오는 값이어도 된다. [running] 이면 받은 값에서 시작해 남은 시간 동안 고르게 1 까지 차서 초 사이가 끊기지 않는다.
 * 시간을 보이는 것이라 곡선 없이 일정한 빠르기다. 동작 줄이기거나 멈춰 있으면 받은 [elapsed] 그대로다.
 * 매 프레임 바뀌는 값이니 그리는 단계에서 읽는다.
 */
@Composable
fun rememberElapsedFraction(elapsed: Duration, total: Duration, running: Boolean): State<Float> {
    val given = (elapsed / total).toFloat().coerceIn(0f, 1f)
    val flowing = running && !MontageMotion.reduceMotion
    val fraction = remember { Animatable(given) }
    val start by rememberUpdatedState(given)
    // 흐르는 동안은 1초마다 오는 값으로 다시 시작하지 않는다. 멈춰 있으면 받은 값마다 맞춘다.
    LaunchedEffect(flowing, if (flowing) Unit else given) {
        fraction.snapTo(start)
        if (flowing) fraction.animateTo(1f, tween(((1f - start) * total.inWholeMilliseconds).roundToInt(), easing = LinearEasing))
    }
    return fraction.asState()
}

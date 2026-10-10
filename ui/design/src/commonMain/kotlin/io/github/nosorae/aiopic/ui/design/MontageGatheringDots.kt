package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

private const val DOTS = 42
private const val CYCLE_SECONDS = 2.8f

/**
 * 기다리는 동안의 점 무리. 흩어져 있던 점이 왼쪽부터 차례로 모여 소리 물결 모양을 이루고, 잠깐 머문 뒤 다시 흩어진다.
 * 질문을 불러오거나 답변을 분석하는 동안, 일이 진행 중이라는 것을 보인다. 무엇을 기다리는지는 옆 문장이 알린다.
 * 동작 줄이기면 물결 모양으로 모인 채 멈춰 있다. [description] 은 화면 읽기 프로그램이 읽는다.
 */
@Composable
fun MontageGatheringDots(description: String, modifier: Modifier = Modifier) {
    val color = MontageTheme.colors.brandDecoration
    val seconds = rememberFrameSeconds()
    val still = MontageMotion.reduceMotion
    val dots = remember { Dots() }
    Canvas(modifier.fillMaxWidth().height(64.dp).semantics { contentDescription = description }) {
        val t = seconds.value
        val mid = size.height / 2
        val radius = size.height * 0.045f
        for (i in 0 until DOTS) {
            val x = dots.x[i]
            val gathered = if (still) 1f else gathering(t, x)
            val waveY = mid - mid * 0.7f * dots.taper[i] * sin(2 * PI.toFloat() * 2.2f * x - t * 1.4f)
            val point = Offset(
                lerp(dots.scatterX[i], x, gathered) * size.width,
                lerp(dots.scatterY[i] * size.height, waveY, gathered),
            )
            drawCircle(color, radius * (0.6f + 0.4f * gathered), point, alpha = 0.3f + 0.7f * gathered)
        }
    }
}

/** 점마다 변하지 않는 값(가로 위치, 양 끝으로 갈수록 줄어드는 물결 높이, 흩어진 위치)을 한 번만 계산해 둔다. */
private class Dots {
    val x = FloatArray(DOTS) { it / (DOTS - 1f) }
    val taper = FloatArray(DOTS) { sin(PI * x[it]).toFloat().pow(1.2f) }
    val scatterX = FloatArray(DOTS) { (x[it] + (noise(it, 1) - 0.5f) * 0.18f).coerceIn(0.02f, 0.98f) }
    val scatterY = FloatArray(DOTS) { 0.1f + 0.8f * noise(it, 2) }
}

/** 한 주기 안에서 x 위치 점이 얼마나 모였는지(0~1). 왼쪽 점이 먼저 모이고 먼저 흩어진다. */
private fun gathering(seconds: Float, x: Float): Float {
    val p = ((seconds / CYCLE_SECONDS) - x * 0.18f).mod(1f)
    val amount = when {
        p < 0.35f -> p / 0.35f
        p < 0.7f -> 1f
        p < 0.95f -> 1f - (p - 0.7f) / 0.25f
        else -> 0f
    }
    return FastOutSlowInEasing.transform(amount)
}

/** 점마다 고정된 0~1 값. 찍을 때마다 흩어진 위치가 같아야 스크린샷이 흔들리지 않는다. */
private fun noise(index: Int, salt: Int): Float {
    val v = sin((index * 12.9898 + salt * 78.233)) * 43758.5453
    return (v - floor(v)).toFloat()
}

private fun lerp(from: Float, to: Float, amount: Float) = from + (to - from) * amount

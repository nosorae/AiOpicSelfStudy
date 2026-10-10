package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private const val SAMPLES = 56
private const val SILENT = 0.12f
private const val STILL_LEVEL = 0.55f

/** 겹 하나. 높이 비율, 물결 수, 흐르는 빠르기(초당 라디안), 위상, 진하기. 뒤 겹일수록 넓고 옅고 느리다. */
private class Layer(val reach: Float, val waves: Float, val speed: Float, val phase: Float, val alpha: Float)

private val layers = listOf(
    Layer(reach = 1.0f, waves = 1.6f, speed = 1.3f, phase = 0f, alpha = 0.22f),
    Layer(reach = 0.78f, waves = 2.3f, speed = -1.9f, phase = 1.7f, alpha = 0.38f),
    Layer(reach = 0.55f, waves = 3.1f, speed = 2.6f, phase = 3.4f, alpha = 0.9f),
)

/**
 * 말하는 동안의 목소리 띠. 주홍 띠 세 겹이 서로 다른 박자로 출렁이고, 목소리가 커지면 띠가 위아래로 부푼다.
 * 녹음되고 있다는 것과 내 목소리가 들어간다는 것을 글을 읽지 않고 보이게 한다. 한 화면의 주인공 움직임으로 쓴다.
 *
 * [level] 은 지금 음량(0~1)이다. 음량이 튀어도 띠는 스프링으로 따라가 부드럽게 부푼다. 조용해도 얇게 숨 쉬듯 남는다.
 * 동작 줄이기면 중간 크기로 멈춘 띠를 보인다.
 */
@Composable
fun MontageVoiceRibbon(level: Float, modifier: Modifier = Modifier) {
    val color = MontageTheme.colors.brandDecoration
    val reduceMotion = MontageMotion.reduceMotion
    val target = if (reduceMotion) STILL_LEVEL else level.coerceIn(0f, 1f)
    val swell by animateFloatAsState(target, MontageMotion.settle())
    val seconds = rememberFrameSeconds()
    val path = remember { Path() }
    val brush = remember(color) { Brush.horizontalGradient(listOf(color.copy(alpha = 0f), color, color.copy(alpha = 0f))) }
    Canvas(modifier.fillMaxWidth().height(72.dp)) {
        val amplitude = SILENT + (1f - SILENT) * swell
        layers.forEach { layer -> drawLayer(path, layer, amplitude, seconds.value, brush) }
    }
}

/** 띠 한 겹: 가운데 선 위아래로 같은 물결을 그려 닫는다. 양 끝은 좁아져 소리가 가운데서 나는 모양이 된다. */
private fun DrawScope.drawLayer(path: Path, layer: Layer, amplitude: Float, seconds: Float, brush: Brush) {
    val mid = size.height / 2
    val half = mid * layer.reach * amplitude
    fun offset(i: Int): Float {
        val x = i / SAMPLES.toFloat()
        val taper = sin(PI * x).toFloat().pow(1.6f)
        val wave = 0.62f + 0.38f * sin(2 * PI * layer.waves * x + layer.speed * seconds + layer.phase).toFloat()
        return half * taper * wave
    }
    path.reset()
    path.moveTo(0f, mid)
    for (i in 0..SAMPLES) path.lineTo(size.width * i / SAMPLES, mid - offset(i))
    for (i in SAMPLES downTo 0) path.lineTo(size.width * i / SAMPLES, mid + offset(i))
    path.close()
    drawPath(path, brush, alpha = layer.alpha)
}

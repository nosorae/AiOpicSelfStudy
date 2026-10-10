package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

private const val QUIET = 0.08f
private const val STILL = 0.3f

/**
 * 녹음 음량 막대. [levels] 는 0 에서 1 사이 음량을 오래된 것부터 담는다. 목소리가 들어가는지 글을 읽지 않고 보이게 한다.
 * 동작 줄이기면 음량과 상관없이 같은 높이로 멈춰 있다.
 */
// ponytail: 새 음량을 그대로 그린다. 기기에서 막대가 튀어 보이면 막대마다 MontageMotion.SHORT_MILLIS 로 따라가게 한다.
@Composable
fun MontageWaveform(levels: List<Float>, modifier: Modifier = Modifier) {
    val color = MontageTheme.colors.brandDecoration
    val heights = waveformHeights(levels, MontageMotion.reduceMotion)
    Canvas(modifier.fillMaxWidth().height(40.dp)) {
        if (heights.isEmpty()) return@Canvas
        val slot = size.width / heights.size
        val bar = slot * 0.5f
        heights.forEachIndexed { index, height ->
            val barHeight = size.height * height
            drawRoundRect(
                color = color,
                topLeft = Offset(index * slot + (slot - bar) / 2, (size.height - barHeight) / 2),
                size = Size(bar, barHeight),
                cornerRadius = CornerRadius(bar / 2),
            )
        }
    }
}

/** 막대 높이(전체 높이에 대한 비율). 조용해도 막대가 사라지지 않게 [QUIET] 아래로 내리지 않는다. */
internal fun waveformHeights(levels: List<Float>, reduceMotion: Boolean): List<Float> = levels.map { if (reduceMotion) STILL else it.coerceIn(QUIET, 1f) }

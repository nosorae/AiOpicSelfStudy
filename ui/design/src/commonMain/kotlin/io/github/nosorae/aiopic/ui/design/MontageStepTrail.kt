package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * 문항 진행 점. 문항마다 점 하나가 있고, 끝낸 문항까지 주홍 선이 이어진다. 한 문항을 끝내면 선이 다음 점까지 뻗고 그 점이 톡 차오른다.
 * 지금 풀 문항 점은 주홍 테두리다. "문항 3 / 15" 를 읽지 않아도 어디쯤인지 보이게 한다.
 * 처음 보일 때는 움직이지 않는다. [done] 은 끝낸 문항 수(0~[total])다. 동작 줄이기면 바로 새 위치다.
 */
@Composable
fun MontageStepTrail(total: Int, done: Int, modifier: Modifier = Modifier) {
    val colors = MontageTheme.colors
    val shown by animateFloatAsState(done.toFloat(), MontageMotion.spec(MontageMotion.LONG_MILLIS))
    Canvas(
        modifier.fillMaxWidth().height(24.dp).semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(done.toFloat(), 0f..total.toFloat(), total)
        },
    ) {
        val slot = size.width / total
        fun x(index: Float) = slot * (index + 0.5f)
        val y = size.height / 2
        val dot = minOf(slot * 0.22f, size.height * 0.22f)
        drawLine(colors.lineNeutralPrimary, Offset(x(0f), y), Offset(x(total - 1f), y), dot * 0.35f)
        if (shown > 1f) drawLine(colors.brandDecoration, Offset(x(0f), y), Offset(x(shown - 1f), y), dot * 0.7f)
        // 지금 풀 문항 표시는 선이 닿은 뒤에 다음 점으로 옮긴다.
        val current = (shown + 0.001f).toInt()
        for (i in 0 until total) {
            val fill = ((shown - i - 0.5f) * 2f).coerceIn(0f, 1f)
            val at = Offset(x(i.toFloat()), y)
            drawCircle(colors.lineNeutralPrimary, dot * 0.7f, at)
            if (fill > 0f) drawCircle(colors.brandDecoration, dot * EaseOutBack.transform(fill), at)
            if (i == current) drawCircle(colors.foregroundBrandPrimary, dot, at, style = Stroke(dot * 0.35f))
        }
    }
}

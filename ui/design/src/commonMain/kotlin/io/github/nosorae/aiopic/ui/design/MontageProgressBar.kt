package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * Montage 선형 진행 막대(높이 2dp). 채운 색은 장식용 강조색 [MontageColors.brandDecoration] 이다.
 * [progress] 는 0 에서 1 사이다. 처음 보일 때는 그 값 그대로이고, 값이 바뀌면 새 값까지 차오른다(문항이 넘어갈 때).
 */
@Composable
fun MontageProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(progress, MontageMotion.spec(MontageMotion.LONG_MILLIS))
    LinearProgressIndicator(
        progress = { shown },
        modifier = modifier.fillMaxWidth().height(2.dp),
        color = MontageTheme.colors.brandDecoration,
        trackColor = MontageTheme.colors.surfaceNeutralSecondary,
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

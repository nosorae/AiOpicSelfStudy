package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * 처음 보일 때 왼쪽 아래에서 오른쪽 위로 그려지는 체크. 키 확인, 저장, 복사가 끝났다는 것을 문구 없이도 보이게 한다.
 * 그림뿐이라 화면 읽기 프로그램에는 옆 문구가 결과를 알린다. 크기는 [modifier] 로 정한다.
 */
@Composable
fun MontageCheckMark(modifier: Modifier = Modifier) {
    val color = MontageTheme.colors.foregroundBrandPrimary
    val reduceMotion = MontageMotion.reduceMotion
    val spec = MontageMotion.spec<Float>(MontageMotion.LONG_MILLIS)
    val drawn = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { drawn.animateTo(1f, spec) }
    Canvas(modifier) {
        val check = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.52f)
            lineTo(size.width * 0.42f, size.height * 0.72f)
            lineTo(size.width * 0.8f, size.height * 0.3f)
        }
        val measure = PathMeasure().apply { setPath(check, false) }
        val part = Path()
        measure.getSegment(0f, measure.length * drawn.value, part)
        drawPath(part, color, style = Stroke(size.minDimension * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

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
 * 끝났다는 표시. 옅은 주홍 원 안에 체크가 왼쪽 아래에서 오른쪽 위로 그려지고, 다 그려지면 둥근 물결이 한 번 퍼지고 사라진다.
 * 키 확인, 저장, 복사가 끝났다는 것을 문구를 읽기 전에 보이게 한다. 그림뿐이라 결과 문구는 옆에 둔다.
 * 크기는 [modifier] 로 정하고, 물결이 퍼질 몫까지 포함한 크기다. 동작 줄이기면 원과 체크가 처음부터 다 그려져 있다.
 * 목록 안에 두면 스크롤로 화면 밖에 나갔다 돌아올 때 다시 그려진다. 그때는 [playOnceKey] 를 줘서 한 번 그린 뒤에는 다 그려진 체크부터 보인다.
 */
@Composable
fun MontageCheckMark(modifier: Modifier = Modifier, playOnceKey: String? = null) {
    val colors = MontageTheme.colors
    val reduceMotion = MontageMotion.reduceMotion
    val drawSpec = MontageMotion.spec<Float>(MontageMotion.LONG_MILLIS)
    val rippleSpec = MontageMotion.spec<Float>(MontageMotion.LONG_MILLIS)
    val played = MontageMotion.played
    val still = remember { reduceMotion || (playOnceKey != null && playOnceKey in played) }
    val drawn = remember { Animatable(if (still) 1f else 0f) }
    val ripple = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        playOnceKey?.let(played::add)
        drawn.animateTo(1f, drawSpec)
        ripple.animateTo(1f, rippleSpec)
    }
    val check = remember { Path() }
    val part = remember { Path() }
    val measure = remember { PathMeasure() }
    Canvas(modifier) {
        val disc = size.minDimension * 0.32f
        drawCircle(colors.surfaceBrandPrimaryOpacity12, disc)
        val r = ripple.value
        if (r in 0.001f..0.999f) {
            drawCircle(colors.brandDecoration, disc + (size.minDimension / 2 - disc) * r, alpha = 1f - r, style = Stroke(disc * 0.08f * (1f - r)))
        }
        check.reset()
        check.moveTo(center.x - disc * 0.42f, center.y + disc * 0.02f)
        check.lineTo(center.x - disc * 0.1f, center.y + disc * 0.32f)
        check.lineTo(center.x + disc * 0.45f, center.y - disc * 0.3f)
        measure.setPath(check, false)
        part.reset()
        measure.getSegment(0f, measure.length * drawn.value, part)
        drawPath(part, colors.foregroundBrandPrimary, style = Stroke(disc * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

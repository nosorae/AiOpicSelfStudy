package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val SPARKS = 10

/**
 * 등급 눈금. [grades] 를 낮은 것부터 왼쪽에서 오른쪽으로 늘어놓고, 빛나는 구슬이 왼쪽 끝에서 미끄러져 와 [low]~[high] 범위 가운데에 멈춘다.
 * 멈추는 순간 작은 불꽃이 한 번 튀고, 범위 막대가 구슬에서 양옆으로 펴진다. 내 등급이 전체 중 어디쯤인지 글자를 읽기 전에 보이게 한다.
 * 리포트 화면의 주인공 움직임으로 쓴다. 동작 줄이기면 구슬과 범위가 처음부터 그 위치에 있다.
 * [low], [high] 는 [grades] 의 번호(0 부터)다. 화면 읽기 프로그램은 [description] 을 읽는다.
 */
@Composable
fun MontageGradeScale(grades: List<String>, low: Int, high: Int, description: String, modifier: Modifier = Modifier) {
    val colors = MontageTheme.colors
    val typography = MontageTheme.typography
    val reduceMotion = MontageMotion.reduceMotion
    val target = (low + high) / 2f
    val slideSpec = MontageMotion.settle<Float>()
    val burstSpec = MontageMotion.spec<Float>(MontageMotion.LONG_MILLIS)
    val orb = remember { Animatable(if (reduceMotion) target else 0f) }
    val burst = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(target) {
        orb.animateTo(target, slideSpec)
        burst.animateTo(1f, burstSpec)
    }
    Column(modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(48.dp)) {
            val slot = size.width / grades.size
            fun x(index: Float) = slot * (index + 0.5f)
            val y = size.height / 2
            val tick = size.height * 0.05f
            drawLine(colors.lineNeutralPrimary, Offset(x(0f), y), Offset(x(grades.lastIndex.toFloat()), y), tick * 0.6f)
            grades.indices.forEach { drawCircle(colors.lineNeutralPrimary, tick, Offset(x(it.toFloat()), y)) }

            val spread = FastOutSlowInEasing.transform(burst.value)
            val center = x(orb.value)
            drawLine(
                colors.brandDecoration.copy(alpha = 0.4f),
                Offset(center + (x(low.toFloat()) - center) * spread, y),
                Offset(center + (x(high.toFloat()) - center) * spread, y),
                strokeWidth = tick * 3.2f,
                cap = StrokeCap.Round,
            )

            val core = size.height * 0.16f
            val b = burst.value
            if (b in 0.001f..0.999f) {
                repeat(SPARKS) { i ->
                    val angle = 2 * PI * i / SPARKS + 0.3
                    val reach = core * (1.2f + 2.6f * b)
                    val at = Offset(center + reach * cos(angle).toFloat(), y + reach * sin(angle).toFloat())
                    drawCircle(colors.brandDecoration, core * 0.16f * (1f - b), at, alpha = 1f - b)
                }
            }
            val glow = core * 2.6f
            drawCircle(Brush.radialGradient(listOf(colors.brandDecoration.copy(alpha = 0.45f), colors.brandDecoration.copy(alpha = 0f)), Offset(center, y), glow), glow, Offset(center, y))
            drawCircle(colors.foregroundBrandPrimary, core, Offset(center, y))
            drawCircle(MontagePalette.Common100.copy(alpha = 0.55f), core * 0.3f, Offset(center - core * 0.32f, y - core * 0.32f))
        }
        Row(Modifier.fillMaxWidth()) {
            grades.forEachIndexed { index, grade ->
                val inRange = index in low..high
                Text(
                    grade,
                    Modifier.weight(1f),
                    style = if (inRange) typography.caption1.bold else typography.caption1.regular,
                    color = if (inRange) colors.foregroundBrandPrimary else colors.foregroundNeutralTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

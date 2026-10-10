package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** 한 문항 안의 단계. 시험 문항은 피드백 없이 다음 문항으로 넘어가서 [Feedback] 은 한 문항 연습만 쓴다. */
enum class AnswerStep(internal val label: String, internal val icon: DrawableResource) {
    Listen("듣기", Res.drawable.icon_normal_play),
    Answer("답하기", Res.drawable.icon_normal_microphone),
    Feedback("피드백", Res.drawable.icon_normal_ai_review),
}

private val NODE = 40.dp
private val ICON = 20.dp

/**
 * 한 문항의 단계 표시. 단계마다 아이콘 원이 있고 원 사이를 선이 잇는다. 지난 단계는 체크, 지금 단계만 강조색으로 찬 원이다.
 * 다음 단계로 가면 선이 먼저 뻗고 다음 원이 차오른다. 규칙 글을 읽지 않아도 지금 무엇을 하는지, 다음에 무엇이 오는지 보이게 한다.
 * 처음 보일 때는 움직이지 않는다. 동작 줄이기면 바로 새 단계다. [withFeedback] 이 아니면 듣기와 답하기만 보인다.
 */
// ponytail: 단계 이름은 앱이 한국어 하나라 여기 둔다. 언어를 더하면 리소스로 옮긴다.
@Composable
fun MontageAnswerSteps(current: AnswerStep, withFeedback: Boolean, modifier: Modifier = Modifier) {
    val steps = if (withFeedback) AnswerStep.entries else AnswerStep.entries - AnswerStep.Feedback
    val reached by animateFloatAsState(current.ordinal.toFloat(), MontageMotion.spec(MontageMotion.LONG_MILLIS))
    val colors = MontageTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { index, step ->
            // 선은 앞 절반, 원은 뒤 절반 동안 찬다.
            val arrive = { ((reached - index + 1) * 2f).coerceIn(0f, 2f) }
            if (index > 0) {
                Canvas(Modifier.weight(1f).padding(top = NODE / 2 - 1.dp).height(2.dp)) {
                    val y = size.height / 2
                    drawLine(colors.lineNeutralPrimary, Offset(0f, y), Offset(size.width, y), size.height)
                    val filled = arrive().coerceAtMost(1f)
                    if (filled > 0f) drawLine(colors.brandDecoration, Offset(0f, y), Offset(size.width * filled, y), size.height, StrokeCap.Round)
                }
            }
            StepNode(step, done = index < current.ordinal, isCurrent = step == current, fill = { (arrive() - 1f).coerceIn(0f, 1f) })
        }
    }
}

@Composable
private fun StepNode(step: AnswerStep, done: Boolean, isCurrent: Boolean, fill: () -> Float) {
    val colors = MontageTheme.colors
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            when {
                done -> stateDescription = "끝남"
                isCurrent -> stateDescription = "지금"
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing6),
    ) {
        val icon = painterResource(if (done) Res.drawable.icon_normal_check_thick else step.icon)
        Canvas(Modifier.size(NODE)) {
            val radius = size.minDimension / 2
            val filled = if (isCurrent) fill() else 0f
            if (done) {
                drawCircle(colors.surfaceBrandPrimaryOpacity12, radius)
            } else {
                drawCircle(colors.lineNeutralPrimary, radius - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                if (filled > 0f) drawCircle(colors.surfaceBrandPrimary, radius * filled)
            }
            // 원이 차오르는 동안 아이콘 색도 함께 바뀐다. 그리는 단계에서만 읽어 매 프레임 다시 구성하지 않는다.
            val tint = if (done) colors.foregroundBrandPrimary else lerp(colors.foregroundNeutralSecondary, colors.onBrand, filled)
            val iconSize = Size(ICON.toPx(), ICON.toPx())
            translate((size.width - iconSize.width) / 2, (size.height - iconSize.height) / 2) {
                with(icon) { draw(iconSize, colorFilter = ColorFilter.tint(tint)) }
            }
        }
        Text(
            text = step.label,
            style = if (isCurrent) MontageTheme.typography.label1.bold else MontageTheme.typography.label1.medium,
            color = if (isCurrent) colors.foregroundNeutralPrimary else colors.foregroundNeutralSecondary,
        )
    }
}

package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.jetbrains.compose.resources.stringResource

/**
 * 질문을 불러오는 동안 문항 화면의 진행 문구 대신 보인다. 말하기 앱이라 음성 파형 막대가 오르내리는 모양이다.
 * 시스템의 동작 줄이기가 켜져 있으면 막대를 멈춘 채 둔다. [text] 는 무엇을 만드는지, 오래 걸리면 연결 상태를 보라는 문구다.
 */
@Composable
internal fun QuestionLoading(text: String, modifier: Modifier = Modifier, animate: Boolean = !rememberReduceMotion()) {
    val description = stringResource(Res.string.exam_question_loading_description)
    Column(modifier.testTag("exam-question-loading"), verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12)) {
        Wave(animate, Modifier.semantics { contentDescription = description })
        Text(
            text = text,
            style = MontageTheme.typography.heading2.bold,
            color = MontageTheme.colors.foregroundNeutralPrimary,
        )
    }
}

/** 막대 다섯 개가 차례로 커졌다 작아진다. 멈춘 모양은 가운데가 높은 고정 파형이다. */
@Composable
private fun Wave(animate: Boolean, modifier: Modifier = Modifier) {
    val transition = if (animate) rememberInfiniteTransition(label = "question-loading") else null
    Row(
        modifier = modifier.height(WaveHeight),
        horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        STILL_WAVE.forEachIndexed { index, still ->
            val scale = transition?.animateFloat(
                initialValue = MIN_SCALE,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    // 한 번 오르고 내리는 데 1초다. 막대마다 0.1초씩 늦춰 물결처럼 보이게 한다.
                    animation = tween(durationMillis = WAVE_MILLIS / 2, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * WAVE_MILLIS / 10),
                ),
                label = "bar-$index",
            )
            Box(
                Modifier
                    .width(MontageSpacing.spacing4)
                    .height(WaveHeight)
                    .graphicsLayer { scaleY = scale?.value ?: still }
                    .clip(RoundedCornerShape(MontageSpacing.spacing2))
                    .background(MontageTheme.colors.foregroundBrandPrimary),
            )
        }
    }
}

private val WaveHeight = MontageSpacing.spacing24
private const val WAVE_MILLIS = 1_000
private const val MIN_SCALE = 0.25f
private val STILL_WAVE = listOf(0.4f, 0.7f, 1f, 0.7f, 0.4f)

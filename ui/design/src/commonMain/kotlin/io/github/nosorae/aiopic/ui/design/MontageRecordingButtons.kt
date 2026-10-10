package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * 문항 녹음 중 아래 버튼 둘(연습과 시험이 같이 쓴다).
 * 다시 듣기는 쓸 수 있는 [replayWindow] 동안 버튼 둘레의 주홍 테두리가 위 가운데부터 시계 방향으로 줄어들고,
 * 시간이 끝나거나 [canReplay] 가 꺼지면 버튼이 옅어지며 접히고 Next 가 그만큼 넓어진다.
 * Next 는 켜지기까지 [nextDelay] 동안 채움이 차오르고, 다 차서 켜지면 누름 진동을 한 번 낸다.
 * 동작 줄이기면 다시 듣기는 바로 사라지고 Next 는 켜지기까지 남은 초를 글자로 보인다.
 * [saving] 은 Next 로 낸 답변을 저장하는 중일 때만 켠다. [tagPrefix] 를 주면 `<prefix>-replay`, `<prefix>-next` 테스트 태그를 단다.
 */
// ponytail: 앱이 한국어 하나라 버튼 이름을 여기 둔다. 언어를 더하면 리소스로 옮긴다.
@Composable
fun MontageRecordingButtons(
    recorded: Duration,
    replayWindow: Duration,
    nextDelay: Duration,
    canReplay: Boolean,
    canNext: Boolean,
    saving: Boolean,
    onReplay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String? = null,
) {
    val replayUsed = rememberElapsedFraction(recorded, replayWindow, running = canReplay)
    val nextFill = rememberElapsedFraction(recorded, nextDelay, running = !canNext)
    val haptics = LocalHaptics.current
    val wasNext = remember { booleanArrayOf(canNext) }
    LaunchedEffect(canNext) {
        if (canNext && !wasNext[0]) haptics.play(HapticEffect.Tap)
        wasNext[0] = canNext
    }
    // 테두리가 다 줄면 창이 끝난 것이다. 1초마다 오는 canReplay 를 기다리지 않고 그때 접기 시작한다.
    val replayOpen by remember(canReplay) { derivedStateOf { canReplay && replayUsed.value < 1f } }
    val share = animateFloatAsState(if (replayOpen) 1f else 0f, MontageMotion.spec(MontageMotion.MEDIUM_MILLIS), label = "replayShare")
    val showReplay by remember { derivedStateOf { share.value > 0f } }
    fun Modifier.tag(name: String) = if (tagPrefix != null) testTag("$tagPrefix-$name") else this
    Row(modifier) {
        if (showReplay) {
            MontageButton(
                text = "다시 듣기",
                onClick = onReplay,
                enabled = canReplay,
                type = MontageButtonType.Assistive,
                modifier = Modifier
                    .folding(share)
                    .shrinkingRing(remaining = { 1f - replayUsed.value })
                    .tag("replay"),
            )
        }
        MontageButton(
            text = if (MontageMotion.reduceMotion && !canNext) "Next (${(nextDelay - recorded).inWholeSeconds}초)" else "Next",
            onClick = onNext,
            enabled = canNext,
            loading = saving,
            fill = if (canNext) null else ({ nextFill.value }),
            modifier = Modifier.weight(1f).tag("next"),
        )
    }
}

/**
 * 줄의 반(사이 간격 포함)을 [share] 만큼만 차지한다. 버튼은 늘 반 폭으로 그리고 넘치는 쪽은 자르며 함께 옅어진다.
 * [share] 는 배치와 그리기 단계에서만 읽어 접히는 동안 다시 구성하지 않는다.
 */
private fun Modifier.folding(share: State<Float>): Modifier = this
    .graphicsLayer { alpha = share.value }
    .clipToBounds()
    .layout { measurable, constraints ->
        val gap = MontageSpacing.spacing8.roundToPx()
        val half = (constraints.maxWidth - gap) / 2
        val placeable = measurable.measure(constraints.copy(minWidth = half, maxWidth = half))
        val width = ((half + gap) * share.value).roundToInt()
        layout(width, placeable.height) { placeable.place(0, 0) }
    }

/** 버튼 둘레 옅은 테두리 위에 위 가운데부터 시계 방향으로 [remaining] 비율만큼 주홍 테두리를 그린다. 남은 다시 듣기 시간이다. */
@Composable
private fun Modifier.shrinkingRing(remaining: () -> Float): Modifier {
    val color = MontageTheme.colors.foregroundBrandPrimary
    val track = MontageTheme.colors.lineNeutralSecondary
    return drawWithCache {
        val stroke = 2.dp.toPx()
        val inset = stroke / 2
        val r = (MontageRadius.radius14.toPx() - inset).coerceAtLeast(0f)
        val l = inset
        val t = inset
        val rt = size.width - inset
        val b = size.height - inset
        val outline = Path().apply {
            moveTo(size.width / 2, t)
            lineTo(rt - r, t)
            arcTo(Rect(rt - 2 * r, t, rt, t + 2 * r), 270f, 90f, false)
            lineTo(rt, b - r)
            arcTo(Rect(rt - 2 * r, b - 2 * r, rt, b), 0f, 90f, false)
            lineTo(l + r, b)
            arcTo(Rect(l, b - 2 * r, l + 2 * r, b), 90f, 90f, false)
            lineTo(l, t + r)
            arcTo(Rect(l, t, l + 2 * r, t + 2 * r), 180f, 90f, false)
            close()
        }
        val measure = PathMeasure().apply { setPath(outline, false) }
        val part = Path()
        onDrawWithContent {
            drawContent()
            drawPath(outline, track, style = Stroke(stroke))
            part.reset()
            measure.getSegment(0f, measure.length * remaining().coerceIn(0f, 1f), part, true)
            drawPath(part, color, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

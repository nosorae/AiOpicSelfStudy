package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.window.Dialog
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect

/**
 * Montage 알림 대화상자. 확인 버튼 하나와 선택인 취소 버튼이 있다.
 * [negative] 면 확인 버튼이 빨강이다. 녹음 지우기처럼 되돌릴 수 없는 동작에 쓴다.
 * 작게 시작해 한 번 살짝 지나쳤다 제 크기로 멈추며 떠서, 화면 위에 새 질문이 올라왔다는 것이 보인다. 동작 줄이기면 처음부터 제 크기다.
 */
@Composable
internal fun MontageAlert(
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    title: String? = null,
    cancel: String? = null,
    onCancel: () -> Unit = onDismissRequest,
    negative: Boolean = false,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        val reduceMotion = MontageMotion.reduceMotion
        val spec = MontageMotion.settle<Float>()
        val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, spec) }
        MontageAlertContent(
            message,
            confirm,
            onConfirm,
            modifier = Modifier.graphicsLayer {
                val v = appear.value
                alpha = v.coerceAtMost(1f)
                scaleX = APPEAR_FROM + (1f - APPEAR_FROM) * v
                scaleY = scaleX
            },
            title = title,
            cancel = cancel,
            onCancel = onCancel,
            negative = negative,
        )
    }
}

private const val APPEAR_FROM = 0.9f

/**
 * 되돌리기 어려운 동작(나가기, 키 지우기, 동의 철회) 전에 묻는 확인 대화상자. 앱의 확인 대화상자는 모두 이것을 쓴다.
 * [message] 에는 확인하면 실제로 무슨 일이 일어나는지 쓴다. 확인 버튼은 빨강이고 누르면 경고 진동을 낸다.
 * 바깥을 누르거나 뒤로가기를 누르면 취소와 같다.
 */
@Composable
fun MontageConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    cancel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val haptics = LocalHaptics.current
    MontageAlert(
        message = message,
        confirm = confirm,
        onConfirm = {
            haptics.play(HapticEffect.Warning)
            onConfirm()
        },
        onDismissRequest = onCancel,
        title = title,
        cancel = cancel,
        negative = true,
    )
}

/**
 * 대화상자 창 없이 내용만 그린다. 스크린샷과 미리보기가 쓴다.
 * 큰 모서리 카드에 굵은 제목과 회색 본문, 아래에 채운 버튼을 같은 너비로 나란히 둔다. 시스템 대화상자 모양을 쓰지 않아 Android 와 iOS 에서 같게 보인다.
 */
@Composable
internal fun MontageAlertContent(
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    cancel: String? = null,
    onCancel: () -> Unit = {},
    negative: Boolean = false,
) {
    val colors = MontageTheme.colors
    val typography = MontageTheme.typography
    Column(
        modifier
            .clip(RoundedCornerShape(MontageRadius.radius24))
            .background(colors.surfaceElevatedPrimary)
            .padding(MontageSpacing.spacing24),
    ) {
        if (title != null) {
            Text(title, style = typography.heading1.bold, color = colors.foregroundNeutralPrimary)
            Spacer(Modifier.height(MontageSpacing.spacing8))
        }
        Text(message, style = typography.body1.regular, color = colors.foregroundNeutralTertiary)
        Spacer(Modifier.height(MontageSpacing.spacing24))
        AlertButtons {
            MontageButton(
                text = confirm,
                onClick = onConfirm,
                modifier = Modifier.testTag("alert-confirm"),
                type = if (negative) MontageButtonType.Negative else MontageButtonType.Primary,
            )
            if (cancel != null) {
                MontageButton(cancel, onCancel, Modifier.testTag("alert-cancel"), type = MontageButtonType.Assistive)
            }
        }
    }
}

/**
 * 버튼을 같은 너비로 나란히 놓는다. 하나라도 제 너비가 한 칸보다 넓으면(긴 문구, 글자 200%) 가로 꽉 차게 세로로 쌓는다.
 * [content] 는 확인, 취소 순이다. 나란히 놓을 때는 취소를 왼쪽에, 쌓을 때는 확인을 위에 둔다.
 */
@Composable
private fun AlertButtons(content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val gap = MontageSpacing.spacing8.roundToPx()
        val width = constraints.maxWidth
        val cell = (width - gap * (measurables.size - 1)) / measurables.size
        val side = measurables.all { it.maxIntrinsicWidth(Constraints.Infinity) <= cell }
        val placeables = measurables.map { it.measure(Constraints.fixedWidth(if (side) cell else width)) }
        if (side) {
            val height = placeables.maxOf { it.height }
            layout(width, height) {
                placeables.asReversed().forEachIndexed { i, p -> p.placeRelative(i * (cell + gap), (height - p.height) / 2) }
            }
        } else {
            layout(width, placeables.sumOf { it.height } + gap * (placeables.size - 1)) {
                var y = 0
                placeables.forEach { p ->
                    p.placeRelative(0, y)
                    y += p.height + gap
                }
            }
        }
    }
}

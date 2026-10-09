package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Montage 알림 대화상자. 확인 버튼 하나와 선택인 취소 버튼이 있다.
 * [negative] 면 확인 버튼이 빨강이다. 녹음 지우기처럼 되돌릴 수 없는 동작에 쓴다.
 */
@Composable
fun MontageAlert(
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
        MontageAlertContent(message, confirm, onConfirm, title = title, cancel = cancel, onCancel = onCancel, negative = negative)
    }
}

/** 대화상자 창 없이 내용만 그린다. 스크린샷과 미리보기가 쓴다. */
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
    Column(modifier.clip(RoundedCornerShape(MontageRadius.radius16)).background(colors.surfaceElevatedPrimary)) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
            if (title != null) Text(title, style = typography.heading2.bold, color = colors.foregroundNeutralPrimary)
            Text(message, style = typography.body2.regular, color = colors.foregroundNeutralTertiary)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8, Alignment.End),
        ) {
            if (cancel != null) {
                MontageButton(cancel, onCancel, variant = MontageButtonVariant.Text, type = MontageButtonType.Assistive)
            }
            MontageButton(
                text = confirm,
                onClick = onConfirm,
                variant = MontageButtonVariant.Text,
                type = if (negative) MontageButtonType.Negative else MontageButtonType.Primary,
            )
        }
    }
}

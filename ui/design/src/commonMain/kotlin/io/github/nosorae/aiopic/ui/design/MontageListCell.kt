package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

/**
 * Montage 목록 행(Full 형태, 좌우 여백 20dp). 눌러서 들어가는 행이면 [onClick] 을 주고 [chevron] 을 켠다.
 * [selected] 면 제목이 강조색 굵은 글자가 되고 오른쪽에 체크가 붙는다.
 * [struck] 이 켜지면 제목 위에 주홍 줄이 왼쪽부터 줄을 따라 그어진다. 교정이 틀렸다고 표시한 것을 빨간 펜으로 긋듯 보인다.
 */
@Composable
fun MontageListCell(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    selected: Boolean = false,
    chevron: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    struck: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = MontageTheme.colors
    val typography = MontageTheme.typography
    val strike = animateFloatAsState(if (struck) 1f else 0f, MontageMotion.spec(MontageMotion.LONG_MILLIS), label = "strike")
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, interactionSource = null, indication = ripple(), onClick = onClick)
                } else {
                    Modifier
                },
            )
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing12),
        horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing2)) {
            Text(
                text = title,
                style = if (selected) typography.body2.bold else typography.body2.medium,
                color = when {
                    !enabled -> colors.foregroundDisablePrimary
                    selected -> colors.foregroundBrandPrimary
                    else -> colors.foregroundNeutralPrimary
                },
                onTextLayout = { layout = it },
                modifier = Modifier.strike(strike, { layout }, colors.foregroundBrandPrimary),
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = typography.label2.regular,
                    color = if (enabled) colors.foregroundNeutralTertiary else colors.foregroundDisablePrimary,
                )
            }
        }
        trailing()
        if (selected) {
            Icon(painterResource(Res.drawable.icon_normal_check), null, Modifier.size(22.dp), tint = colors.foregroundBrandPrimary)
        }
        if (chevron) {
            Icon(
                painterResource(Res.drawable.icon_normal_chevron_right_small),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (enabled) colors.foregroundNeutralQuaternary else colors.foregroundDisablePrimary,
            )
        }
    }
}

/** 글자 줄마다 가운데를 따라 [progress] 비율만큼 줄을 긋는다. 여러 줄이면 첫 줄부터 이어 긋는다. 값은 그리는 단계에서만 읽는다. */
private fun Modifier.strike(progress: State<Float>, layout: () -> TextLayoutResult?, color: Color) = drawWithContent {
    drawContent()
    val text = layout() ?: return@drawWithContent
    val lines = 0 until text.lineCount
    var left = lines.sumOf { (text.getLineRight(it) - text.getLineLeft(it)).toDouble() }.toFloat() * progress.value
    for (line in lines) {
        if (left <= 0f) break
        val start = text.getLineLeft(line)
        val end = minOf(text.getLineRight(line), start + left)
        val y = (text.getLineTop(line) + text.getLineBottom(line)) / 2
        drawLine(color, Offset(start, y), Offset(end, y), 2.dp.toPx(), StrokeCap.Round)
        left -= end - start
    }
}

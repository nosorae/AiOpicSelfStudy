package io.github.nosorae.aiopic.ui.design

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

/**
 * Montage 목록 행(Full 형태, 좌우 여백 20dp). 눌러서 들어가는 행이면 [onClick] 을 주고 [chevron] 을 켠다.
 * [selected] 면 제목이 강조색 굵은 글자가 되고 오른쪽에 체크가 붙는다.
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
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = MontageTheme.colors
    val typography = MontageTheme.typography
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

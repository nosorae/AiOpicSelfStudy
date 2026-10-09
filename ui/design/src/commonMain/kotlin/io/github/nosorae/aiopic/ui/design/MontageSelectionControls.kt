package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

/** Montage 체크박스(18dp, 모서리 5dp). 눌리는 영역은 48dp 다. */
@Composable
fun MontageCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MontageTheme.colors
    val shape = RoundedCornerShape(5.dp)
    val fill = when {
        !checked -> Color.Transparent
        enabled -> colors.surfaceBrandPrimary
        else -> colors.surfaceDisablePrimary
    }
    val border = when {
        checked -> fill
        enabled -> colors.lineNeutralPrimary
        else -> colors.lineNeutralTertiary
    }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = null,
                indication = ripple(bounded = false, radius = 20.dp),
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(18.dp).background(fill, shape).border(1.5.dp, border, shape), contentAlignment = Alignment.Center) {
            if (checked) {
                Icon(
                    painterResource(Res.drawable.icon_normal_check_thick),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (enabled) colors.onBrand else colors.foregroundDisablePrimary,
                )
            }
        }
    }
}

/** Montage 라디오(20dp). 고르면 테두리가 6dp 강조색 고리가 된다. 눌리는 영역은 48dp 다. */
@Composable
fun MontageRadio(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MontageTheme.colors
    val ring = when {
        !enabled -> colors.lineNeutralTertiary
        selected -> colors.surfaceBrandPrimary
        else -> colors.lineNeutralPrimary
    }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = null,
                indication = ripple(bounded = false, radius = 20.dp),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(20.dp).border(if (selected) 6.dp else 1.5.dp, ring, CircleShape))
    }
}

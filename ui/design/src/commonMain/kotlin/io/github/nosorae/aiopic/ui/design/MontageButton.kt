package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class MontageButtonVariant { Solid, Outlined, Text }

/** Assistive 는 회색 보조 버튼, Negative 는 지우기처럼 되돌릴 수 없는 동작이다(Solid 만). */
enum class MontageButtonType { Primary, Assistive, Negative }

/** Text 변형은 배경이 없어 Montage 가 높이를 따로 둔다([textMinHeight]). */
enum class MontageButtonSize(
    internal val minHeight: Dp,
    internal val textMinHeight: Dp,
    internal val horizontalPadding: Dp,
    internal val iconSize: Dp,
) {
    Large(48.dp, 40.dp, 20.dp, 20.dp),
    Medium(40.dp, 32.dp, 16.dp, 18.dp),
    Small(32.dp, 28.dp, 12.dp, 16.dp),
}

/**
 * Montage 버튼. 모양은 Montage 값을 따르고, 눈에 보이는 높이가 48dp 보다 작아도 눌리는 영역은 48dp 로 넓힌다.
 */
@Composable
fun MontageButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MontageButtonVariant = MontageButtonVariant.Solid,
    type: MontageButtonType = MontageButtonType.Primary,
    size: MontageButtonSize = MontageButtonSize.Large,
    enabled: Boolean = true,
    leadingIcon: DrawableResource? = null,
) {
    val colors = MontageTheme.colors
    val typography = MontageTheme.typography
    val content = buttonContentColor(variant, type, enabled)
    val background = when {
        variant != MontageButtonVariant.Solid -> Color.Transparent
        !enabled -> colors.surfaceDisablePrimary
        type == MontageButtonType.Assistive -> colors.surfaceNeutralSecondary
        type == MontageButtonType.Negative -> colors.foregroundNegativePrimaryOpacity12
        else -> colors.surfaceBrandPrimary
    }
    val isText = variant == MontageButtonVariant.Text
    val shape = RoundedCornerShape(
        when (size) {
            MontageButtonSize.Large -> if (isText) MontageRadius.radius10 else MontageRadius.radius14
            MontageButtonSize.Medium -> if (isText) MontageRadius.radius8 else MontageRadius.radius12
            MontageButtonSize.Small -> if (isText) MontageRadius.radius8 else MontageRadius.radius10
        },
    )
    val textStyle = when {
        isText && size == MontageButtonSize.Small -> typography.label1.bold
        isText -> typography.body1.bold
        size == MontageButtonSize.Large -> typography.body2.bold
        size == MontageButtonSize.Medium -> typography.label1.bold
        else -> typography.caption1.bold
    }
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(background)
            .then(
                if (variant == MontageButtonVariant.Outlined) {
                    Modifier.border(BorderStroke(1.dp, colors.lineNeutralSecondary), shape)
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = null,
                indication = ripple(color = content),
                onClick = onClick,
            )
            .heightIn(min = if (isText) size.textMinHeight else size.minHeight)
            .padding(horizontal = if (isText) 7.dp else size.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing4, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(painterResource(leadingIcon), contentDescription = null, tint = content, modifier = Modifier.size(size.iconSize))
        }
        Text(text = text, style = textStyle, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun buttonContentColor(variant: MontageButtonVariant, type: MontageButtonType, enabled: Boolean): Color {
    val colors = MontageTheme.colors
    return when {
        !enabled -> colors.foregroundDisablePrimary

        variant == MontageButtonVariant.Solid -> when (type) {
            MontageButtonType.Primary -> colors.onBrand
            MontageButtonType.Assistive -> colors.foregroundNeutralSecondary
            MontageButtonType.Negative -> colors.foregroundNegativeStrong
        }

        variant == MontageButtonVariant.Outlined && type == MontageButtonType.Assistive -> colors.foregroundNeutralPrimary

        variant == MontageButtonVariant.Text && type == MontageButtonType.Assistive -> colors.foregroundNeutralTertiary

        type == MontageButtonType.Negative -> colors.foregroundNegativePrimary

        else -> colors.foregroundBrandPrimary
    }
}

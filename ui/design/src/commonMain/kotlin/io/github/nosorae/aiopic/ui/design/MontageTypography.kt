package io.github.nosorae.aiopic.ui.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Montage v4.0.0 글자 스케일. 크기마다 regular, medium, bold 세 굵기가 있다.
 * display, title 의 bold 는 700, heading 아래는 600 이다(Montage 값 그대로).
 */
@Immutable
class MontageTypography internal constructor(family: FontFamily) {
    val display1 = MontageTextStyles(family, 56, 72, -0.0319f, FontWeight.Bold)
    val display2 = MontageTextStyles(family, 40, 52, -0.0282f, FontWeight.Bold)
    val display3 = MontageTextStyles(family, 36, 48, -0.027f, FontWeight.Bold)
    val title1 = MontageTextStyles(family, 32, 44, -0.0253f, FontWeight.Bold)
    val title2 = MontageTextStyles(family, 28, 38, -0.0236f, FontWeight.Bold)
    val title3 = MontageTextStyles(family, 24, 32, -0.023f, FontWeight.Bold)
    val heading1 = MontageTextStyles(family, 22, 30, -0.0194f)
    val heading2 = MontageTextStyles(family, 20, 28, -0.012f)
    val headline1 = MontageTextStyles(family, 18, 26, -0.002f)
    val headline2 = MontageTextStyles(family, 17, 24, 0f)
    val body1 = MontageTextStyles(family, 16, 24, 0.0057f)
    val body1Reading = MontageTextStyles(family, 16, 26, 0.0057f)
    val body2 = MontageTextStyles(family, 15, 22, 0.0096f)
    val body2Reading = MontageTextStyles(family, 15, 24, 0.0096f)
    val label1 = MontageTextStyles(family, 14, 20, 0.0145f)
    val label1Reading = MontageTextStyles(family, 14, 22, 0.0145f)
    val label2 = MontageTextStyles(family, 13, 18, 0.0194f)
    val caption1 = MontageTextStyles(family, 12, 16, 0.0252f)
    val caption2 = MontageTextStyles(family, 11, 14, 0.0311f)
}

@Immutable
class MontageTextStyles internal constructor(
    family: FontFamily,
    size: Int,
    lineHeight: Int,
    letterSpacingEm: Float,
    boldWeight: FontWeight = FontWeight.SemiBold,
) {
    private val base = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacingEm.em,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
    val regular = base.copy(fontWeight = FontWeight.Normal)
    val medium = base.copy(fontWeight = FontWeight.Medium)
    val bold = base.copy(fontWeight = boldWeight)
}

// 테마 값은 화면 어디서나 꺼내 쓰므로 CompositionLocal 둘(색, 글자)로 내린다. 이 둘 말고는 만들지 않는다.
@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.Font

private val LocalMontageColors = staticCompositionLocalOf { MontageLightColors }
private val LocalMontageTypography = staticCompositionLocalOf<MontageTypography> {
    error("MontageTheme 안에서만 글자 스타일을 쓸 수 있다")
}

/**
 * Montage 색과 Pretendard 글자를 내려 준다. [darkTheme] 은 설정의 화면 모드(F9.1)에서 정해 넘긴다.
 *
 * Material 3 색 역할에도 Montage 색을 넣는다. 앱 틀의 네이티브 하단 탭(Android Material 3)이 같은 색을 쓰게 하려는 것이다.
 */
@Composable
fun MontageTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) MontageDarkColors else MontageLightColors
    val family = FontFamily(
        Font(Res.font.pretendard_regular, FontWeight.Normal),
        Font(Res.font.pretendard_medium, FontWeight.Medium),
        Font(Res.font.pretendard_semi_bold, FontWeight.SemiBold),
        Font(Res.font.pretendard_bold, FontWeight.Bold),
    )
    val typography = remember(family) { MontageTypography(family) }
    val scheme = colors.toMaterial(darkTheme)
    CompositionLocalProvider(LocalMontageColors provides colors, LocalMontageTypography provides typography) {
        MaterialTheme(colorScheme = scheme) {
            CompositionLocalProvider(LocalContentColor provides colors.foregroundNeutralPrimary) {
                ProvideTextStyle(typography.body1.regular, content)
            }
        }
    }
}

object MontageTheme {
    val colors: MontageColors
        @Composable @ReadOnlyComposable
        get() = LocalMontageColors.current

    val typography: MontageTypography
        @Composable @ReadOnlyComposable
        get() = LocalMontageTypography.current
}

/** Montage 간격 토큰. 화면의 여백과 간격은 여기서만 꺼낸다. */
object MontageSpacing {
    val spacing2 = 2.dp
    val spacing4 = 4.dp
    val spacing6 = 6.dp
    val spacing8 = 8.dp
    val spacing12 = 12.dp
    val spacing16 = 16.dp
    val spacing20 = 20.dp
    val spacing24 = 24.dp
    val spacing32 = 32.dp
    val spacing40 = 40.dp
    val spacing48 = 48.dp
}

/** Montage 모서리 반경 토큰. */
object MontageRadius {
    val radius4 = 4.dp
    val radius8 = 8.dp
    val radius10 = 10.dp
    val radius12 = 12.dp
    val radius14 = 14.dp
    val radius16 = 16.dp
    val radius20 = 20.dp
    val radius24 = 24.dp
}

private fun MontageColors.toMaterial(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = surfaceBrandPrimary,
        onPrimary = onBrand,
        background = backgroundNeutralPrimary,
        onBackground = foregroundNeutralPrimary,
        surface = backgroundNeutralPrimary,
        onSurface = foregroundNeutralPrimary,
        onSurfaceVariant = foregroundNeutralTertiary,
        secondary = foregroundBrandPrimary,
        secondaryContainer = surfaceBrandPrimaryOpacity12,
        onSecondaryContainer = foregroundBrandPrimary,
        surfaceContainer = backgroundNeutralSecondary,
        error = foregroundNegativePrimary,
    )
} else {
    lightColorScheme(
        primary = surfaceBrandPrimary,
        onPrimary = onBrand,
        background = backgroundNeutralPrimary,
        onBackground = foregroundNeutralPrimary,
        surface = backgroundNeutralPrimary,
        onSurface = foregroundNeutralPrimary,
        onSurfaceVariant = foregroundNeutralTertiary,
        secondary = foregroundBrandPrimary,
        secondaryContainer = surfaceBrandPrimaryOpacity12,
        onSecondaryContainer = foregroundBrandPrimary,
        surfaceContainer = backgroundNeutralSecondary,
        error = foregroundNegativePrimary,
    )
}

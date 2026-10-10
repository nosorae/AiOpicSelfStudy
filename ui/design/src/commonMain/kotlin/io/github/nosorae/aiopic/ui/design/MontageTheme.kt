// 테마 값은 화면 어디서나 꺼내 쓰므로 CompositionLocal 넷(색, 글자, 동작 줄이기, 이미 움직인 표시)으로 내린다. 이 넷 말고는 만들지 않는다.
@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.Font

private val LocalMontageColors = staticCompositionLocalOf { MontageLightColors }
private val LocalReduceMotion = staticCompositionLocalOf { false }
private val LocalPlayedMotions = staticCompositionLocalOf { mutableSetOf<String>() }
private val LocalMontageTypography = staticCompositionLocalOf<MontageTypography> {
    error("MontageTheme 안에서만 글자 스타일을 쓸 수 있다")
}

/**
 * Montage 색과 Pretendard 글자를 내려 준다. [darkTheme] 은 설정의 화면 모드(F9.1)에서 정해 넘긴다.
 * [reduceMotion] 은 Android 애니메이션 제거, iOS 동작 줄이기 설정이다. 켜지면 [MontageMotion.spec] 이 바로 끝 값으로 건너뛴다.
 * [playedMotions] 는 한 번만 움직이는 부품(등급 눈금, 점수 숫자)이 이미 움직인 표시다. 앱이 떠 있는 동안 하나를 써야
 * 스크롤, 뒤로 가기, 탭 다시 열기로 다시 그려도 같은 등급과 점수가 또 움직이지 않는다. 화면마다 테마를 따로 띄우는 iOS 는 앱 틀이 하나를 들고 넘긴다.
 *
 * Material 3 색 역할에도 Montage 색을 넣는다. 앱 틀의 네이티브 하단 탭(Android Material 3)이 같은 색을 쓰게 하려는 것이다.
 */
// 이미 움직인 표시는 화면이 바뀌어도 다시 그릴 일이 없는 기록이라(처음 그릴 때 한 번 읽고, 움직이기 시작할 때 넣는다) 상태로 만들지 않는다.
@Suppress("ktlint:compose:mutable-params-check")
@Composable
fun MontageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = false,
    playedMotions: MutableSet<String> = rememberSaveable(saver = playedSaver) { mutableSetOf() },
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) MontageDarkColors else MontageLightColors
    val family = FontFamily(
        Font(Res.font.pretendard_regular, FontWeight.Normal),
        Font(Res.font.pretendard_medium, FontWeight.Medium),
        Font(Res.font.pretendard_semi_bold, FontWeight.SemiBold),
        Font(Res.font.pretendard_bold, FontWeight.Bold),
    )
    val typography = remember(family) { MontageTypography(family) }
    val scheme = colors.toMaterial(darkTheme)
    CompositionLocalProvider(
        LocalMontageColors provides colors,
        LocalMontageTypography provides typography,
        LocalReduceMotion provides reduceMotion,
        LocalPlayedMotions provides playedMotions,
    ) {
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

/**
 * 움직임 길이와 곡선(`ui-quality` 2절). 화면은 `tween`, `spring` 을 직접 부르지 않고 [spec], [settle] 을 쓴다.
 * 값이 바뀌는 것은 빨리 출발해 천천히 멈추는 곡선 하나, 정해진 위치로 가서 멈추는 것은 살짝 지나쳤다 돌아오는 스프링 하나다.
 */
object MontageMotion {
    /** 선택 표시, 버튼 상태처럼 작은 표시. */
    const val SHORT_MILLIS = 150

    /** 화면 안 요소가 바뀔 때 기본값. */
    const val MEDIUM_MILLIS = 250

    /** 진행 막대 차오름, 숫자 올라가기, 체크 그리기처럼 눈으로 따라가야 하는 것. */
    const val LONG_MILLIS = 400

    /** 동작 줄이기가 켜져 있는지. 계속 도는 표시를 멈춘 그림으로 바꿀 때 읽는다. */
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReduceMotion.current

    /** [durationMillis] 동안 움직이는 곡선. 동작 줄이기면 바로 끝 값이다. */
    @Composable @ReadOnlyComposable
    fun <T> spec(durationMillis: Int = MEDIUM_MILLIS): FiniteAnimationSpec<T> = if (reduceMotion) snap() else tween(durationMillis, easing = FastOutSlowInEasing)

    /** 등급 구슬, 진행 점처럼 정해진 위치로 가서 멈추는 것. 한 번 살짝 지나쳤다 1초 안에 멈춘다. 동작 줄이기면 바로 끝 값이다. */
    @Composable @ReadOnlyComposable
    fun <T> settle(): FiniteAnimationSpec<T> = if (reduceMotion) snap() else spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow)

    /** [MontageTheme] 의 이미 움직인 표시. 부품은 처음 그릴 때 자기 키가 여기 있으면 끝 장면부터 그리고, 움직이기 시작할 때 키를 넣는다. */
    internal val played: MutableSet<String>
        @Composable @ReadOnlyComposable
        get() = LocalPlayedMotions.current
}

private val playedSaver = listSaver<MutableSet<String>, String>(save = { it.toList() }, restore = { it.toMutableSet() })

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

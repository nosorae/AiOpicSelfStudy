package io.github.nosorae.aiopic.libs.debug

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 디버깅 화면의 토큰. 이 모듈은 다른 앱에도 붙어야 해서 앱 디자인 체계를 쓰지 않고 색, 글자, 간격, 모양을 여기 둔다.
 * 화면 코드는 색 리터럴, 글자 크기, 간격 숫자를 직접 쓰지 않고 여기서 꺼낸다.
 *
 * 색은 차가운 회색 바탕에 강조색 하나(코발트)다. 상태 색 넷(성공, 이동, 요청 오류, 서버 오류와 실패)은 네트워크 상태 코드와
 * 로그 단계에만 쓴다. 문제를 찾는 화면이라 눈이 가는 색은 문제가 있는 줄에만 남긴다. 글자색은 바탕 위에서 4.5:1 이상이다.
 */
@Immutable
internal class DebugColors(
    val background: Color,
    val raised: Color,
    val sunken: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val ok: Color,
    val warn: Color,
    val fail: Color,
) {
    /** 상태 색 위에 까는 옅은 바탕. 글자는 같은 상태 색으로 쓴다. */
    fun tint(color: Color) = color.copy(alpha = 0.10f)
}

private val Light = DebugColors(
    background = Color(0xFFF5F6F8),
    raised = Color(0xFFFFFFFF),
    sunken = Color(0xFFEAEDF1),
    ink = Color(0xFF161A22),
    muted = Color(0xFF545D6E),
    line = Color(0xFFD7DBE2),
    accent = Color(0xFF2C55C9),
    ok = Color(0xFF1B7543),
    warn = Color(0xFF8F5300),
    fail = Color(0xFFBC2F28),
)

private val Dark = DebugColors(
    background = Color(0xFF111419),
    raised = Color(0xFF181C23),
    sunken = Color(0xFF0B0E12),
    ink = Color(0xFFE4E8EE),
    muted = Color(0xFF9AA3B2),
    line = Color(0xFF2A3039),
    accent = Color(0xFF93AAFF),
    ok = Color(0xFF62CF8F),
    warn = Color(0xFFEAB15A),
    fail = Color(0xFFFF7D74),
)

/**
 * 지금 테마의 토큰 색. [DebugTheme] 이 바탕색을 토큰에서 넣어서 바탕색으로 라이트와 다크를 가린다.
 * ponytail: CompositionLocal 을 새로 만들지 않으려고 바탕색을 표지로 쓴다. 테마가 둘보다 많아지면 CompositionLocal 로 바꾼다.
 */
internal val debugColors: DebugColors
    @Composable get() = if (MaterialTheme.colorScheme.background == Dark.background) Dark else Light

/** 기록(로그, 주소, 본문)은 고정폭으로 읽고 화면 틀(탭, 버튼, 제목)은 시스템 글꼴로 읽는다. 단위가 sp 라서 글자 크기 설정을 따른다. */
internal object DebugType {
    private val mono = FontFamily.Monospace
    val title = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
    val section = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 22.sp)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val code = TextStyle(fontFamily = mono, fontSize = 13.sp, lineHeight = 19.sp)
    val codeStrong = code.copy(fontWeight = FontWeight.Bold)
    val codeSmall = TextStyle(fontFamily = mono, fontSize = 12.sp, lineHeight = 16.sp)
}

/** 4dp 단위 간격. 묶인 것 사이는 좁게, 묶음 사이는 넓게 둔다. */
internal object DebugSpace {
    val bar = 3.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp

    /** 누르는 줄과 버튼의 최소 높이. 두 플랫폼 기준 중 큰 쪽이다. */
    val touch = 48.dp

    /** 상태 코드 칩의 최소 너비. 세 자리 숫자가 줄마다 같은 너비로 세로로 줄을 맞춘다. */
    val statusChip = 44.dp
}

internal object DebugShape {
    val chip = RoundedCornerShape(4.dp)
    val block = RoundedCornerShape(8.dp)
}

/** 시스템 다크 모드를 따른다. Material 부품(탭, 스위치, 입력칸)도 같은 색으로 그리게 색 구성을 토큰에서 만든다. */
@Composable
internal fun DebugTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) Dark else Light
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = c.accent,
        onPrimary = c.raised,
        secondaryContainer = c.tint(c.accent),
        onSecondaryContainer = c.ink,
        background = c.background,
        onBackground = c.ink,
        surface = c.background,
        onSurface = c.ink,
        surfaceVariant = c.sunken,
        onSurfaceVariant = c.muted,
        surfaceContainerLowest = c.raised,
        surfaceContainerLow = c.raised,
        surfaceContainer = c.raised,
        surfaceContainerHigh = c.raised,
        surfaceContainerHighest = c.sunken,
        outline = c.muted,
        outlineVariant = c.line,
        error = c.fail,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

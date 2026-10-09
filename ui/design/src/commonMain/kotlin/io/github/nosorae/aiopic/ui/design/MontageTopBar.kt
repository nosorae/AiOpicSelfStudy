package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Montage 상단 바(높이 56dp, 제목 왼쪽 정렬). 배경은 상태 바 아래까지 그리고 내용만 안전 영역 안에 둔다.
 * 뒤로가기는 플랫폼이 그리므로(ADR 0018) [navigationIcon] 은 닫기처럼 화면 안 동작에만 쓴다.
 */
@Composable
fun MontageTopBar(
    title: String,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .background(MontageTheme.colors.backgroundNeutralPrimary)
            .windowInsetsPadding(windowInsets)
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationIcon?.invoke()
        Text(
            text = title,
            style = MontageTheme.typography.headline2.bold,
            color = MontageTheme.colors.foregroundNeutralStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = if (navigationIcon == null) 12.dp else 0.dp),
        )
        actions()
    }
}

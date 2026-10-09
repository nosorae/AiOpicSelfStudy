package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 공용 화면 틀. 배경은 시스템 바 아래까지 그리고, 내용은 시스템 바, 컷아웃, 키보드 안쪽에 둔다(ADR 0021).
 * 화면은 인셋을 따로 계산하지 않는다. 위쪽 인셋은 [topBar] 의 [MontageTopBar] 가 맡고,
 * 나머지는 여기서 맡는다. [bottomBar] 는 키보드가 올라오면 키보드 바로 위에 붙는다.
 */
@Composable
fun MontageScreen(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxSize().background(MontageTheme.colors.backgroundNeutralPrimary)) {
        topBar()
        Box(Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            content()
        }
        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))) {
            bottomBar()
        }
    }
}

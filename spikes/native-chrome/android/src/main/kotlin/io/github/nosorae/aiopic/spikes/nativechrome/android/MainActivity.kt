package io.github.nosorae.aiopic.spikes.nativechrome.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import io.github.nosorae.aiopic.spikes.nativechrome.shared.Platform
import io.github.nosorae.aiopic.spikes.nativechrome.shared.Screen
import io.github.nosorae.aiopic.spikes.nativechrome.shared.ScreenContent
import io.github.nosorae.aiopic.spikes.nativechrome.shared.showsTabBar
import io.github.nosorae.aiopic.spikes.nativechrome.shared.topLevelScreens

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 확인용: 실행 인자로 시작 경로를 정한다. 예) --es start ReportDetail
        val start = intent.getStringExtra("start")?.let(Screen::valueOf)
        setContent { MaterialTheme { AppChrome(start) } }
    }
}

// 앱 틀: 플랫폼 코드는 여기 하나. 탭을 보일지는 공통 경로 표(showsTabBar)가 정한다.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppChrome(start: Screen?) {
    var tab by rememberSaveable { mutableStateOf(Screen.Home) }
    val stacks = remember {
        topLevelScreens.associateWith { root ->
            mutableStateListOf(root).apply {
                if (root == Screen.Home && start != null) {
                    if (start == Screen.Exam) add(Screen.ReportDetail)
                    add(start)
                }
            }
        }
    }
    val backStack = stacks.getValue(tab)
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (backStack.last().showsTabBar(Platform.Android)) {
                ShortNavigationBar {
                    topLevelScreens.forEach { screen ->
                        ShortNavigationBarItem(
                            selected = tab == screen,
                            onClick = { tab = screen },
                            icon = { Icon(iconOf(screen), contentDescription = null) },
                            label = { Text(screen.title) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeAt(backStack.lastIndex) },
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            entryProvider = { screen -> NavEntry(screen) { ScreenContent(screen, onOpen = { backStack.add(it) }) } },
        )
    }
}

private fun iconOf(screen: Screen) = when (screen) {
    Screen.History -> Icons.Filled.DateRange
    Screen.Settings -> Icons.Filled.Settings
    else -> Icons.Filled.Home
}

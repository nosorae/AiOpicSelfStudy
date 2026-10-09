package io.github.nosorae.aiopic.apps.opic.shared

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.Res
import io.github.nosorae.aiopic.ui.design.icon_normal_calendar
import io.github.nosorae.aiopic.ui.design.icon_normal_home
import io.github.nosorae.aiopic.ui.design.icon_normal_setting
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource

/**
 * Android 앱 틀. 하단 탭(Material 3 Expressive ShortNavigationBar)과 예측 뒤로가기(Navigation 3)를 그린다.
 * 탭을 보일지와 뒤로가기를 막을지는 [AppRoute] 표가 정하고, Android 플랫폼 코드는 이 파일 하나다(ADR 0018).
 *
 * 백스택은 하나다. Android 는 하위 화면에서 탭을 숨겨서 탭을 옮기는 건 늘 탭 화면에 있을 때뿐이라 탭마다 스택을 둘 일이 없다.
 * 홈이 아닌 탭은 홈 위에 쌓아 뒤로가기로 홈에 돌아오게 한다. [startStack] 은 테스트가 시작 화면을 정할 때만 쓴다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppShell(graph: AppGraph, modifier: Modifier = Modifier, startStack: List<AppRoute> = listOf(AppRoute.Home)) {
    val backStack = rememberSaveable(saver = backStackSaver) { startStack.toMutableStateList() }
    val top = backStack.last()
    MontageTheme {
        Scaffold(
            modifier = modifier,
            // 인셋은 화면(MontageScreen)이 맡는다. 탭 바가 있을 때만 그 높이를 여백으로 주고 소비한다.
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (top.showsTabBar(Platform.Android)) {
                    ShortNavigationBar {
                        AppRoute.tabs.forEach { tab ->
                            ShortNavigationBarItem(
                                selected = top == tab,
                                onClick = { backStack.selectTab(tab) },
                                icon = { Icon(painterResource(tabIcon(tab)), contentDescription = null) },
                                label = { Text(tab.title) },
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
                entryProvider = { route ->
                    NavEntry(route) {
                        // 시험 중에는 시스템 뒤로가기(예측 뒤로가기 포함)가 시험을 벗어나지 않게 받아서 버린다.
                        BackHandler(enabled = route.blocksBack) {}
                        RouteContent(
                            route = route,
                            graph = graph,
                            onOpen = { backStack.add(it) },
                            onExit = { backStack.retainAll { it == AppRoute.Home } },
                        )
                    }
                },
            )
        }
    }
}

private fun SnapshotStateList<AppRoute>.selectTab(tab: AppRoute) {
    val next = listOfNotNull(AppRoute.Home, tab.takeIf { it != AppRoute.Home })
    clear()
    addAll(next)
}

private fun tabIcon(tab: AppRoute) = when (tab) {
    AppRoute.History -> Res.drawable.icon_normal_calendar
    AppRoute.Settings -> Res.drawable.icon_normal_setting
    else -> Res.drawable.icon_normal_home
}

private val backStackSaver = listSaver<SnapshotStateList<AppRoute>, String>(
    save = { stack -> stack.map { Json.encodeToString(AppRoute.serializer(), it) } },
    restore = { saved -> saved.map { Json.decodeFromString(AppRoute.serializer(), it) }.toMutableStateList() },
)

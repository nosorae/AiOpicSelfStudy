package io.github.nosorae.aiopic.apps.opic.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * iOS 앱 틀(`ContentView.swift` 의 SwiftUI TabView, NavigationStack)이 목적지마다 Compose 화면을 하나씩 얻어 간다.
 * 탭 표시와 뒤로 버튼은 Swift 가 [AppRoute] 표를 읽어 정한다. 그래프는 앱에 하나다.
 */
class IosShell {
    private val graph = createAppGraph(
        AppSettings(
            read = { NSUserDefaults.standardUserDefaults.stringForKey(it) },
            write = { key, value -> NSUserDefaults.standardUserDefaults.setObject(value, forKey = key) },
        ),
    )

    fun controller(route: AppRoute, onOpen: (AppRoute) -> Unit, onExit: () -> Unit, onBack: () -> Unit): UIViewController = ComposeUIViewController {
        val themeMode by graph.themeModeStore.themeMode.collectAsState()
        // SwiftUI 가 그리는 탭 바와 내비게이션 바도 같은 모드를 따르게 창의 모드를 바꾼다(F9.1).
        LaunchedEffect(themeMode) { applyToWindows(themeMode) }
        MontageTheme(darkTheme = themeMode.isDark(isSystemInDarkTheme())) { RouteContent(route, graph, onOpen, onExit, onBack) }
    }

    fun showsTabBar(route: AppRoute): Boolean = route.showsTabBar(Platform.Ios)

    private fun applyToWindows(mode: ThemeMode) {
        val style = when (mode) {
            ThemeMode.System -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            ThemeMode.Light -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            ThemeMode.Dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { it.windows.filterIsInstance<UIWindow>() }
            .forEach { it.overrideUserInterfaceStyle = style }
    }
}

package io.github.nosorae.aiopic.apps.opic.shared

import androidx.compose.ui.window.ComposeUIViewController
import io.github.nosorae.aiopic.ui.design.MontageTheme
import platform.UIKit.UIViewController

/**
 * iOS 앱 틀(`ContentView.swift` 의 SwiftUI TabView, NavigationStack)이 목적지마다 Compose 화면을 하나씩 얻어 간다.
 * 탭 표시와 뒤로 버튼은 Swift 가 [AppRoute] 표를 읽어 정한다. 그래프는 앱에 하나다.
 */
class IosShell {
    private val graph = createAppGraph()

    fun controller(route: AppRoute, onOpen: (AppRoute) -> Unit, onExit: () -> Unit): UIViewController = ComposeUIViewController {
        MontageTheme { RouteContent(route, graph, onOpen, onExit) }
    }

    fun showsTabBar(route: AppRoute): Boolean = route.showsTabBar(Platform.Ios)
}

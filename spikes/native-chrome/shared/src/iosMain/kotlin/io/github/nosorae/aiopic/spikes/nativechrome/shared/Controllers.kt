package io.github.nosorae.aiopic.spikes.nativechrome.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// Swift 의 TabView, NavigationStack 목적지마다 하나씩 만든다.
fun screenController(
    screen: Screen,
    onOpen: (Screen) -> Unit,
): UIViewController = ComposeUIViewController { ScreenContent(screen, onOpen) }

fun showsTabBarOnIos(screen: Screen): Boolean = screen.showsTabBar(Platform.Ios)

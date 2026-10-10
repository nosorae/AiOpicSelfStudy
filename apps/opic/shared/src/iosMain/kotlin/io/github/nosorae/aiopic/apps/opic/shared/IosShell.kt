package io.github.nosorae.aiopic.apps.opic.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import io.github.nosorae.aiopic.apps.opic.shared.data.opicDatabase
import io.github.nosorae.aiopic.apps.opic.shared.data.recordingsRoot
import io.github.nosorae.aiopic.libs.keyvault.api.KeychainKeyVault
import io.github.nosorae.aiopic.libs.recorder.api.IosRecorder
import io.github.nosorae.aiopic.libs.tts.IosTts
import io.github.nosorae.aiopic.libs.workqueue.api.ForegroundWorkQueue
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
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
    private val keyVault = KeychainKeyVault().also(UiTestLaunch::resetIfAsked)

    private val graph: AppGraph = createAppGraph(
        settings = AppSettings(
            read = { NSUserDefaults.standardUserDefaults.stringForKey(it) },
            write = { key, value -> NSUserDefaults.standardUserDefaults.setObject(value, forKey = key) },
        ),
        keyVault = keyVault,
        database = opicDatabase(),
        recordingsRoot = recordingsRoot(),
        recorder = IosRecorder(),
        tts = IosTts(),
        // 대기열이 그래프보다 먼저 만들어지므로 처리기는 꺼낼 때 그래프에서 찾는다. 앱이 떠 있는 동안만 보낸다(F5.1).
        workQueue = ForegroundWorkQueue(handler = { graph.analysisHandler.handle(it) }, scope = MainScope()),
        gemini = UiTestLaunch.geminiClient(),
    )

    /** 시작 분기(S0, F1.0). 저장된 키와 동의만 읽어 첫 화면을 [onDecided] 로 넘긴다. 그 전까지 Swift 는 실행 화면과 같은 빈 화면만 둔다. */
    fun decideStart(onDecided: (AppRoute) -> Unit) {
        MainScope().launch { onDecided(AppRoute.start(graph.decideStartUseCase())) }
    }

    fun controller(route: AppRoute, onOpen: (AppRoute) -> Unit, onExit: () -> Unit, onBack: () -> Unit, onRestart: (AppRoute) -> Unit): UIViewController = ComposeUIViewController {
        val themeMode by graph.themeModeStore.themeMode.collectAsState()
        // SwiftUI 가 그리는 탭 바와 내비게이션 바도 같은 모드를 따르게 창의 모드를 바꾼다(F9.1).
        LaunchedEffect(themeMode) { applyToWindows(themeMode) }
        MontageTheme(darkTheme = themeMode.isDark(isSystemInDarkTheme())) { RouteContent(route, graph, onOpen, onExit, onBack, onRestart) }
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

import SwiftUI
import Shared

@main
struct iOSApp: App {
    // 그래프(DB, 분석 대기열)는 앱에 하나여야 한다. 화면 구조체는 여러 번 다시 만들어지므로 앱이 한 번만 만든다.
    private let shell = IosShell()

    var body: some Scene {
        WindowGroup {
            ContentView(shell: shell)
        }
    }
}

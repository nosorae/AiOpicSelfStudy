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

// 기기를 흔들면 디버깅 화면을 연다. 시뮬레이터의 흔들기(Device > Shake)는 가속도 센서가 아니라 UIKit 이벤트라서 창에서 받는다.
// 릴리스 빌드는 빈 모듈이라 아무 일도 안 한다.
extension UIWindow {
    open override func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
        super.motionEnded(motion, with: event)
        if motion == .motionShake { IosShellKt.openDebugTools() }
    }
}

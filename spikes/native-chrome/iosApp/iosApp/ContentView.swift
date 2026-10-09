import SwiftUI
import SpikeShared

struct ComposeScreen: UIViewControllerRepresentable {
    let screen: Screen
    let onOpen: (Screen) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        ControllersKt.screenController(screen: screen, onOpen: onOpen)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// 앱 틀: 플랫폼 코드는 여기 하나. 탭과 내비게이션 바를 보일지는 공통 경로 표가 정한다.
struct TabRoot: View {
    let root: Screen
    @Binding var path: [Screen]

    var body: some View {
        NavigationStack(path: $path) {
            destination(root)
                .navigationDestination(for: Screen.self) { destination($0) }
        }
    }

    private func destination(_ screen: Screen) -> some View {
        ComposeScreen(screen: screen, onOpen: { path.append($0) })
            .ignoresSafeArea()
            .navigationTitle(screen.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(screen.chrome == .sub ? .visible : .hidden, for: .navigationBar)
            .toolbar(ControllersKt.showsTabBarOnIos(screen: screen) ? .automatic : .hidden, for: .tabBar)
    }
}

struct ContentView: View {
    @State private var tab: Screen = .home
    @State private var paths: [Screen: [Screen]] = ContentView.initialPaths()

    var body: some View {
        TabView(selection: $tab) {
            ForEach(ChromeKt.topLevelScreens, id: \.self) { root in
                Tab(root.title, systemImage: icon(root), value: root) {
                    TabRoot(root: root, path: Binding(
                        get: { paths[root] ?? [] },
                        set: { paths[root] = $0 }
                    ))
                }
            }
        }
        .task {
            // 확인용: -pushExam 이면 2초 뒤 홈 탭에 시험 화면을 쌓는다(사용자가 버튼을 누른 것과 같은 경로).
            if CommandLine.arguments.contains("-pushExam") {
                try? await Task.sleep(for: .seconds(2))
                paths[.home, default: []].append(.exam)
            }
            // 확인용: -tabSwitch 면 기록 탭에 갔다가 홈으로 돌아온다.
            guard CommandLine.arguments.contains("-tabSwitch") else { return }
            try? await Task.sleep(for: .seconds(2))
            tab = .history
            try? await Task.sleep(for: .seconds(2))
            tab = .home
        }
    }

    private func icon(_ screen: Screen) -> String {
        switch screen {
        case .history: "calendar"
        case .settings: "gearshape"
        default: "house"
        }
    }

    // 확인용: -start ReportDetail 또는 -start Exam 으로 홈 탭의 시작 경로를 정한다.
    private static func initialPaths() -> [Screen: [Screen]] {
        guard let i = CommandLine.arguments.firstIndex(of: "-start"), i + 1 < CommandLine.arguments.count else { return [:] }
        switch CommandLine.arguments[i + 1] {
        case "ReportDetail": return [.home: [.reportdetail]]
        case "Exam": return [.home: [.reportdetail, .exam]]
        default: return [:]
        }
    }
}

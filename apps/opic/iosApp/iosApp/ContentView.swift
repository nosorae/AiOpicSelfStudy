import SwiftUI
import Shared

struct RouteView: UIViewControllerRepresentable {
    let shell: IosShell
    let route: AppRoute
    let onOpen: (AppRoute) -> Void
    let onExit: () -> Void
    let onBack: () -> Void
    let onRestart: (AppRoute) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        shell.controller(route: route, onOpen: onOpen, onExit: onExit, onBack: onBack, onRestart: onRestart)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// iOS 앱 틀. iOS 플랫폼 코드는 이 파일 하나다. 탭, 내비게이션 바, 뒤로가기는 Kotlin 의 화면 경로 표(AppRoute)가 정한다.
struct TabRoot: View {
    let shell: IosShell
    let root: AppRoute
    @Binding var path: [AppRoute]
    var onRestart: (AppRoute) -> Void = { _ in }

    var body: some View {
        NavigationStack(path: $path) {
            destination(root)
                .navigationDestination(for: AppRoute.self) { destination($0) }
        }
    }

    private func destination(_ route: AppRoute) -> some View {
        RouteView(shell: shell, route: route, onOpen: { path.append($0) }, onExit: { path.removeAll() }, onBack: { path.removeLast() }, onRestart: onRestart)
            // 배경은 안전 영역 밖까지 그리고, 내용은 Compose 화면이 safeDrawing 인셋으로 안쪽에 둔다.
            .ignoresSafeArea()
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            // 뒤로 버튼을 숨기면 가장자리 스와이프도 꺼진다. 시험은 화면 안 나가기로만 나간다.
            .navigationBarBackButtonHidden(!route.showsBackButtonOnIos)
            .toolbar(route.showsBackButtonOnIos ? .visible : .hidden, for: .navigationBar)
            // 보일 때 .visible 을 주면 첫 화면 설정이 이겨 시험 화면에도 탭이 남는다. .automatic 이어야 숨는다.
            .toolbar(shell.showsTabBar(route: route) ? .automatic : .hidden, for: .tabBar)
    }
}

// 시작 분기(S0, F1.0)가 첫 화면을 정할 때까지 실행 화면과 같은 빈 배경만 둔다. 키 입력과 동의는 탭 없이 혼자 뜨고, 마치면 분기를 다시 한다.
struct ContentView: View {
    let shell: IosShell
    @State private var start: AppRoute?
    @State private var startPath: [AppRoute] = []

    var body: some View {
        if let first = start, first == AppRoute.Home.shared {
            Tabs(shell: shell)
        } else if let first = start {
            TabRoot(shell: shell, root: first, path: $startPath, onRestart: { next in
                startPath = []
                start = next
            })
            // 루트가 바뀌면 그 Compose 화면을 새로 만든다.
            .id(first)
        } else {
            Color(.systemBackground)
                .ignoresSafeArea()
                .task { shell.decideStart { start = $0 } }
        }
    }
}

struct Tabs: View {
    let shell: IosShell
    @State private var tab: AppRoute = AppRoute.Home.shared
    @State private var paths: [AppRoute: [AppRoute]] = [:]

    var body: some View {
        TabView(selection: $tab) {
            ForEach(AppRoute.companion.tabs, id: \.self) { root in
                Tab(root.title, systemImage: icon(root), value: root) {
                    TabRoot(shell: shell, root: root, path: Binding(
                        get: { paths[root] ?? [] },
                        set: { paths[root] = $0 }
                    ))
                }
            }
        }
    }

    private func icon(_ tab: AppRoute) -> String {
        switch tab {
        case AppRoute.History.shared: "calendar"
        case AppRoute.Settings.shared: "gearshape"
        default: "house"
        }
    }
}

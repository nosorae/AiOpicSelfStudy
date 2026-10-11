import Network
import XCTest

// 분석 중에 앱을 내려도 분석이 끊기지 않는지 화면 터치로 본다(#216).
// 녹음 분석 요청은 시스템 백그라운드 전송으로 이 테스트가 띄운 서버(127.0.0.1:8765)에 간다. 시뮬레이터는 Mac 의 네트워크를 같이 쓴다.
// 서버는 일부러 늦게 답한다. 그 사이 홈으로 나갔다 돌아오면 분석 결과가 보이고, 서버는 요청을 한 번만 받아야 한다.
final class BackgroundAnalysisUITests: XCTestCase {
    private let app = XCUIApplication()
    private var server: SlowAnalysisServer!

    override func setUpWithError() throws {
        continueAfterFailure = false
        server = try SlowAnalysisServer(port: 8765, delay: 8)
    }

    override func tearDown() {
        server.stop()
    }

    func testLeavingAppDuringAnalysisStillShowsResult() {
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini + UITestLaunch.localUpload
        app.launch()
        app.passFirstLaunch()

        tapWhenReady(app.buttons["한 문항 풀기"])
        tapWhenReady(app.buttons["Play"])
        let next = app.buttons["Next"]
        XCTAssertTrue(waitUntil(60) { next.exists && next.isEnabled }, "Next 가 60초 안에 켜지지 않았다")
        next.tap()
        XCTAssertTrue(label(containing: "분석하고 있습니다").waitForExistence(timeout: 30), "보내는 중이 보이지 않는다")
        attach("홈으로 나가기 전")

        XCUIDevice.shared.press(.home)
        XCTAssertTrue(app.wait(for: .runningBackgroundSuspended, timeout: 30) || app.state == .runningBackground, "앱이 뒤로 가지 않았다")
        // 서버가 답하는 8초보다 오래 밖에 있는다.
        Thread.sleep(forTimeInterval: 15)
        app.activate()

        XCTAssertTrue(label(containing: "I live in Seoul.").waitForExistence(timeout: 20), "돌아온 뒤 분석 결과가 보이지 않는다")
        attach("돌아온 뒤 분석 결과")
        XCTAssertEqual(server.requests, 1, "같은 분석을 두 번 보냈다")
    }

    private func tapWhenReady(_ element: XCUIElement) {
        XCTAssertTrue(waitUntil(20) { element.exists && element.isHittable && element.isEnabled }, "\(element) 를 누를 수 없다")
        element.tap()
    }

    private func label(containing text: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
    }

    private func attach(_ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }
}

/** 분석 요청 본문을 다 받고 [delay] 초 뒤에 출력 검사를 통과하는 분석 결과를 돌려주는 HTTP 서버. 받은 요청 수를 센다. */
final class SlowAnalysisServer {
    private let listener: NWListener
    private let queue = DispatchQueue(label: "slow-analysis-server")
    private let delay: TimeInterval
    private var count = 0

    var requests: Int { queue.sync { count } }

    init(port: UInt16, delay: TimeInterval) throws {
        self.delay = delay
        listener = try NWListener(using: .tcp, on: NWEndpoint.Port(rawValue: port)!)
        listener.newConnectionHandler = { [weak self] connection in self?.serve(connection) }
        let ready = DispatchSemaphore(value: 0)
        listener.stateUpdateHandler = { if case .ready = $0 { ready.signal() } }
        listener.start(queue: queue)
        _ = ready.wait(timeout: .now() + 5)
    }

    func stop() {
        listener.cancel()
    }

    private func serve(_ connection: NWConnection) {
        connection.start(queue: queue)
        read(connection, Data())
    }

    /** 헤더의 Content-Length 만큼 본문을 다 받을 때까지 읽는다. */
    private func read(_ connection: NWConnection, _ received: Data) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 1 << 20) { [weak self] data, _, done, error in
            guard let self else { return }
            let all = received + (data ?? Data())
            if let head = all.range(of: Data("\r\n\r\n".utf8)),
               let header = String(data: all[..<head.lowerBound], encoding: .utf8),
               let length = header.split(separator: "\r\n").first(where: { $0.lowercased().hasPrefix("content-length:") })
                .flatMap({ Int($0.split(separator: ":")[1].trimmingCharacters(in: .whitespaces)) }),
               all.count - head.upperBound >= length {
                count += 1
                queue.asyncAfter(deadline: .now() + delay) { self.respond(connection) }
            } else if !done, error == nil {
                read(connection, all)
            }
        }
    }

    private func respond(_ connection: NWConnection) {
        let body = Data(Self.analysis.utf8)
        let head = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: \(body.count)\r\nConnection: close\r\n\r\n"
        connection.send(content: Data(head.utf8) + body, completion: .contentProcessed { _ in connection.cancel() })
    }

    /** 앱의 디버깅 가짜 Gemini 가 연결이 돌아온 뒤 주는 분석 결과와 같다. */
    private static let analysis = #"{"candidates":[{"content":{"parts":[{"text":"{\"transcript\":\"I live in Seoul.\",\"corrections\":[],\"upgrades\":[],\"tenseConsistent\":true,\"sentenceCount\":1,\"wordsPerMinute\":120.0,\"pauses\":[],\"pronunciation\":\"Clear.\",\"score\":80}"}]}}]}"#
}

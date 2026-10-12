import XCTest

// 분석 중에 앱을 내려 두면 끝 알림이 뜨고, 누르면 모의고사 탭에 그 리포트가 열리는지 화면 터치로 본다(#229).
// 분석 요청은 이 테스트가 띄운 서버(BackgroundAnalysisUITests 의 SlowAnalysisServer)에 가고, 서버는 40초 뒤에 답한다.
// 뒤로 간 앱이 받는 시간(30초 안팎)보다 길어서, 그 시간이 끝날 때 비우기가 끊기면 다시 비우기까지 30초를 더 쉬어 알림이 늦게 뜬다.
// 그래서 알림이 서버 답 뒤 곧바로(Next 부터 55초 안) 떠야 통과한다.
final class AnalysisNoticeUITests: XCTestCase {
    private let app = XCUIApplication()
    private let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
    private var server: SlowAnalysisServer!

    override func setUpWithError() throws {
        continueAfterFailure = false
        server = try SlowAnalysisServer(port: 8765, delay: 40)
    }

    override func tearDown() {
        server.stop()
    }

    func testDoneNoticeOpensReportOnExamTab() {
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini + UITestLaunch.localUpload
        app.launch()
        app.passFirstLaunch()

        app.tabBars.buttons["연습"].tap()
        tapWhenReady(app.buttons["Play"])
        let next = app.buttons["Next"]
        XCTAssertTrue(waitUntil(60) { next.exists && next.isEnabled }, "Next 가 60초 안에 켜지지 않았다")
        next.tap()
        let sent = Date()
        // 첫 분석을 시작하면 알림 권한을 묻는다. 시스템은 앱을 지우기 전까지 한 번만 묻는다.
        let allow = springboard.alerts.buttons.matching(NSPredicate(format: "label IN %@", ["허용", "Allow"])).firstMatch
        if allow.waitForExistence(timeout: 10) { allow.tap() }

        // 리포트가 아닌 탭으로 옮겨 둔다. 알림을 누른 뒤 모의고사 탭의 리포트가 보이면 알림이 연 것이다.
        app.tabBars.buttons["설정"].tap()
        XCTAssertTrue(app.staticTexts["분석 모델"].waitForExistence(timeout: 10), "설정 탭이 보이지 않는다")
        XCUIDevice.shared.press(.home)
        attach(springboard, "앱을 내린 뒤")

        let banner = springboard.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "리포트가 준비됐어요")).firstMatch
        XCTAssertTrue(banner.waitForExistence(timeout: 55 - Date().timeIntervalSince(sent)), "Next 부터 55초 안에 끝 알림이 뜨지 않았다. 앱을 내린 뒤 비우기가 끊겼다")
        attach(springboard, "끝 알림")
        banner.tap()

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 10), "알림을 눌러도 앱이 열리지 않는다")
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "I live in Seoul.")).firstMatch.waitForExistence(timeout: 10), "알림을 눌러 연 화면에 분석 결과가 없다")
        XCTAssertTrue(app.tabBars.buttons["모의고사"].isSelected, "알림을 눌러 연 리포트가 모의고사 탭에 없다")
        attach(app, "알림으로 연 리포트")
        XCTAssertEqual(server.requests, 1, "같은 분석을 두 번 보냈다")
    }

    private func tapWhenReady(_ element: XCUIElement) {
        XCTAssertTrue(waitUntil(20) { element.exists && element.isHittable && element.isEnabled }, "\(element) 를 누를 수 없다")
        element.tap()
    }

    private func attach(_ target: XCUIApplication, _ name: String) {
        let shot = XCTAttachment(screenshot: target.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }
}

import XCTest

// 디버그 빌드의 설정 화면 "개발자 도구" 줄로 디버깅 화면을 열고, 키 확인 요청이 Network 탭에 남았는지 본다(#171).
// 흔들기는 XCUITest 가 일으킬 수 없어 설정 줄로 연다.
final class DebugToolsUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini
        app.launch()
    }

    func testSettingsRowOpensNetworkLogWithKeyCheck() throws {
        app.passFirstLaunch()

        tapWhenReady(app.tabBars.buttons["설정"])
        app.tapTag("settings-developer-tools")

        tapWhenReady(app.descendants(matching: .any).matching(NSPredicate(format: "label == 'Network'")).firstMatch)
        let call = app.descendants(matching: .any).matching(NSPredicate(format: "label BEGINSWITH 'POST 200'")).firstMatch
        XCTAssertTrue(call.waitForExistence(timeout: 10), "Network 탭에 키 확인 요청이 보이지 않는다")
        let url = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS 'generateContent'")).firstMatch
        XCTAssertTrue(url.exists, "요청 주소가 보이지 않는다")
        attachScreenshot("디버깅 화면 Network")
    }

    private func tapWhenReady(_ element: XCUIElement) {
        XCTAssertTrue(waitUntil(20) { element.exists && element.isHittable && element.isEnabled }, "\(element) 를 누를 수 없다")
        element.tap()
    }

    private func attachScreenshot(_ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }
}

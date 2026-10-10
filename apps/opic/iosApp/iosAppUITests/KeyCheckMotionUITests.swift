import XCTest

// 키 확인이 3초 걸리는 동안 무엇을 기다리는지 문장이 보이고, 확인되면 저장 버튼 자리에 체크와 결과가 보인 뒤 동의 화면으로 넘어가는지 화면 터치로 본다(#158).
final class KeyCheckMotionUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = [UITestLaunch.reset, UITestLaunch.fakeGemini, UITestLaunch.slowGemini]
        app.launch()
    }

    func testKeyCheckShowsWaitThenCheck() {
        let field = app.tag("key-field")
        XCTAssertTrue(field.waitForExistence(timeout: 30), "키 입력 화면이 보이지 않는다")
        field.tap()
        app.typeText("test-key")
        app.tapTag("key-submit")

        let waiting = label(containing: "키가 맞는지 묻고 있습니다")
        XCTAssertTrue(waiting.waitForExistence(timeout: 2), "키 확인 중 기다림 문장이 보이지 않는다")
        attach("키 확인 중")

        let saved = label(containing: "키를 확인했습니다")
        XCTAssertTrue(saved.waitForExistence(timeout: 10), "키 확인 뒤 체크와 결과 문구가 보이지 않는다")
        XCTAssertFalse(app.tag("key-submit").exists, "확인된 뒤에도 저장 버튼이 남아 있다")
        attach("키 확인됨")

        XCTAssertTrue(app.tag("consent-agree").waitForExistence(timeout: 10), "체크 뒤 동의 화면으로 넘어가지 않았다")
    }

    /** Compose 글자는 iOS 에서 여러 요소에 나뉘어 나올 수 있어 글자를 품은 요소를 찾는다. */
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

import XCTest

// 한 문항 연습을 화면 터치로 끝까지 돈다(#134). 홈 > 한 문항 풀기 > Play > 녹음 10초 > Next > 답변 리포트 > 기록 탭.
// 처음 실행이라 키가 없으면 테스트 실행기 환경 변수 GEMINI_API_KEY 의 키를 붙여 넣는다(xcodebuild 에 TEST_RUNNER_GEMINI_API_KEY 로 준다).
// 키는 테스트 기록에 남지 않게 글자로 치지 않고 붙여 넣는다.
final class PracticeFlowUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        // 마이크 권한 창은 다른 프로세스(SpringBoard)가 띄운다. 허용을 누른다.
        addUIInterruptionMonitor(withDescription: "마이크 권한") { alert in
            for label in ["허용", "Allow", "확인", "OK"] where alert.buttons[label].exists {
                alert.buttons[label].tap()
                return true
            }
            return false
        }
        app.launch()
    }

    func testPracticeNextOpensAnswerReport() throws {
        passStartIfNeeded()

        tapWhenReady(app.buttons["한 문항 풀기"])
        tapWhenReady(app.buttons["Play"])
        // 권한 창이 떠 있으면 앱을 한 번 건드려야 interruption monitor 가 돈다.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01)).tap()

        // 질문 읽기와 녹음 10초가 지나야 Next 가 켜진다.
        let next = app.buttons["Next"]
        XCTAssertTrue(waitUntil(60) { next.exists && next.isEnabled }, "Next 가 60초 안에 켜지지 않았다")
        next.tap()

        XCTAssertTrue(app.staticTexts["답변 리포트"].waitForExistence(timeout: 10), "Next 뒤 답변 리포트가 보이지 않는다")
        XCTAssertFalse(app.buttons["Play"].exists, "Next 뒤에도 연습 화면이 남아 있다")
        attachScreenshot("답변 리포트")

        app.tabBars.buttons["기록"].tap()
        XCTAssertTrue(app.staticTexts["기록"].waitForExistence(timeout: 10))
        attachScreenshot("기록")
    }

    private func passStartIfNeeded() {
        let keyField = app.textViews["키를 붙여 넣으세요"]
        let home = app.buttons["한 문항 풀기"]
        _ = waitUntil(15) { keyField.exists || home.exists || self.app.buttons["동의하고 계속하기"].exists }
        if keyField.exists {
            let key = try? XCTUnwrap(ProcessInfo.processInfo.environment["GEMINI_API_KEY"], "키가 없는 첫 실행이다. TEST_RUNNER_GEMINI_API_KEY 를 준다")
            UIPasteboard.general.string = key
            tapWhenReady(app.buttons["Gemini"])
            keyField.tap()
            keyField.press(forDuration: 1.2)
            let paste = app.descendants(matching: .any).matching(NSPredicate(format: "label IN %@", ["붙여넣기", "Paste"])).firstMatch
            tapWhenReady(paste)
            UIPasteboard.general.string = ""
            tapWhenReady(app.buttons["키 확인하고 저장"])
        }
        let agree = app.buttons["동의하고 계속하기"]
        if agree.waitForExistence(timeout: 20) { agree.tap() }
    }

    private func tapWhenReady(_ element: XCUIElement) {
        XCTAssertTrue(waitUntil(20) { element.exists && element.isHittable && element.isEnabled }, "\(element) 를 누를 수 없다")
        element.tap()
    }

    private func waitUntil(_ seconds: TimeInterval, _ condition: @escaping () -> Bool) -> Bool {
        let predicate = NSPredicate { _, _ in condition() }
        return XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: predicate, object: nil)], timeout: seconds) == .completed
    }

    private func attachScreenshot(_ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }
}

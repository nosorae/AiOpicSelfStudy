import XCTest

// 한 문항 연습을 화면 터치로 끝까지 돈다(#134). 홈 > 한 문항 풀기 > Play > 녹음 10초 > Next > 답변 리포트 > 기록 탭.
// 첫 실행처럼 비우고 가짜 Gemini 로 띄운다. 분석은 가짜 응답을 못 읽어 실패하지만 리포트 화면으로 가는지만 본다.
final class PracticeFlowUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini
        app.launch()
    }

    func testPracticeNextOpensAnswerReport() throws {
        app.passFirstLaunch()

        tapWhenReady(app.buttons["한 문항 풀기"])
        tapWhenReady(app.buttons["Play"])

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

import XCTest

// 답변 리포트에 내용 교정과 최종 스크립트가 보이는지 화면 터치로 본다(#217).
// 가짜 Gemini 는 첫 분석을 끊고 30초 뒤 다시 보낸 요청에 두 항목이 든 결과를 돌려준다. 스크롤해 최종 스크립트를 찾고 복사를 누른다.
final class FinalScriptUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini + UITestLaunch.dropOnce
        app.launch()
        app.passFirstLaunch()
    }

    func testReportShowsContentFixAndFinalScript() {
        tapWhenReady(app.buttons["한 문항 풀기"])
        tapWhenReady(app.buttons["Play"])
        let next = app.buttons["Next"]
        XCTAssertTrue(waitUntil(60) { next.exists && next.isEnabled }, "Next 가 60초 안에 켜지지 않았다")
        next.tap()

        XCTAssertTrue(label(containing: "I live in Seoul.").waitForExistence(timeout: 120), "분석 결과가 보이지 않는다")
        XCTAssertTrue(scrollTo(label(containing: "표현 교정")), "표현 교정 칸이 보이지 않는다")
        let fix = label(containing: "질문에 답하지 않고")
        XCTAssertTrue(scrollTo(fix), "내용 교정이 보이지 않는다")
        attach("내용 교정")

        let script = app.tag("final-script")
        XCTAssertTrue(scrollTo(app.tag("script-copy")), "최종 스크립트의 복사 버튼이 보이지 않는다")
        XCTAssertTrue(script.label.contains("everything I need is close"), "최종 스크립트 글이 다르다: \(script.label)")
        app.tag("script-copy").tap()
        let copied = label(containing: "복사했습니다")
        XCTAssertTrue(copied.waitForExistence(timeout: 5), "복사한 뒤 안내가 보이지 않는다")
        XCTAssertTrue(scrollTo(copied), "복사 안내가 탭 바 밑에 가려 있다")
        attach("최종 스크립트 복사")
    }

    private func tapWhenReady(_ element: XCUIElement) {
        XCTAssertTrue(waitUntil(20) { element.exists && element.isHittable && element.isEnabled }, "\(element) 를 누를 수 없다")
        element.tap()
    }

    private func scrollTo(_ element: XCUIElement) -> Bool {
        for _ in 0..<8 where !(element.exists && element.isHittable) {
            app.swipeUp()
        }
        return element.exists && element.isHittable
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

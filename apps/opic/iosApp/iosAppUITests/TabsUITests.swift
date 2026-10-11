import XCTest

// 하단 탭 넷(모의고사, 연습, 기록, 설정)을 터치로 오가고, 모의고사 탭의 시작과 연습 탭의 Play 가 각각 시작되는지 본다(#215).
// 첫 실행처럼 비우고 가짜 Gemini 로 띄운다.
final class TabsUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini
        app.launch()
    }

    func testFourTabsEachStart() {
        app.passFirstLaunch()
        XCTAssertEqual(app.tabBars.buttons.allElementsBoundByIndex.map(\.label), ["모의고사", "연습", "기록", "설정"])

        // 모의고사: 시작을 누르면 탭 없는 시험이 서베이부터 열리고, 나가면 모의고사 탭으로 돌아온다.
        XCTAssertTrue(app.tabBars.buttons["모의고사"].isSelected, "첫 탭이 모의고사가 아니다")
        attachScreenshot("모의고사 탭")
        app.tapTag("exam-start")
        XCTAssertTrue(app.tag("survey-option-work-0").waitForExistence(timeout: 30), "시작을 눌러도 서베이가 열리지 않는다")
        XCTAssertFalse(app.tabBars.firstMatch.isHittable, "시험 화면에 탭이 보인다")
        app.tapTag("exam-exit")
        app.tapTag("alert-confirm")
        XCTAssertTrue(app.tag("exam-resume").waitForExistence(timeout: 30), "나간 회차의 이어 보기가 보이지 않는다")
        attachScreenshot("모의고사 탭 이어 보기")

        // 연습: Play 한 번이면 연습 화면이 열리고 바로 질문을 읽어 녹음까지 간다.
        app.tabBars.buttons["연습"].tap()
        XCTAssertTrue(app.staticTexts["듣기"].waitForExistence(timeout: 10), "연습 탭이 보이지 않는다")
        attachScreenshot("연습 탭")
        tapWhenReady(app.buttons["Play"])
        XCTAssertTrue(app.staticTexts["한 문항 연습"].waitForExistence(timeout: 10), "Play 를 눌러도 연습 화면이 열리지 않는다")
        let next = app.buttons["Next"]
        XCTAssertTrue(waitUntil(60) { next.exists }, "Play 한 번으로 질문 읽기와 녹음이 시작되지 않았다")
        app.tapTag("practice-exit")
        app.tapTag("alert-confirm")
        XCTAssertTrue(app.tabBars.buttons["연습"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.tabBars.buttons["연습"].isSelected, "연습을 나간 뒤 연습 탭으로 돌아오지 않았다")

        app.tabBars.buttons["기록"].tap()
        XCTAssertTrue(app.staticTexts["기록"].waitForExistence(timeout: 10), "기록 탭이 보이지 않는다")
        app.tabBars.buttons["설정"].tap()
        XCTAssertTrue(app.staticTexts["분석 모델"].waitForExistence(timeout: 10), "설정 탭이 보이지 않는다")
        app.tabBars.buttons["모의고사"].tap()
        XCTAssertTrue(app.tag("exam-resume").waitForExistence(timeout: 10), "모의고사 탭으로 돌아오지 않았다")
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

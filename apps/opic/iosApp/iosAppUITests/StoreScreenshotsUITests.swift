import XCTest

// App Store 스크린샷(#236). 예시 기록을 넣고 띄워 모의고사 탭, 회차 리포트, 답변 리포트, 기록, 녹음 중인 문항을 찍는다.
// iPhone 17 Pro Max(6.9형, 1320x2868)에서 돌린다. 환경 변수 STORE_SCREENSHOTS_DIR 가 있으면 그 폴더에 PNG 로 쓰고, 없으면 첨부로만 남긴다.
// 사용법은 README 의 "스토어 스크린샷".
final class StoreScreenshotsUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini + ["ui-test-store-demo"]
        app.launch()
    }

    func testStoreScreenshots() throws {
        app.passFirstLaunch()
        let latest = app.staticTexts["모의고사 3회차"]
        XCTAssertTrue(latest.waitForExistence(timeout: 30), "예시 기록이 보이지 않는다")
        try save("1_exam_tab")

        latest.tap()
        XCTAssertTrue(app.staticTexts["IM3 ~ IH"].waitForExistence(timeout: 10), "회차 리포트가 보이지 않는다")
        settle()
        try save("3_session_report")

        app.staticTexts["2번 묘사"].tap()
        XCTAssertTrue(app.staticTexts["원문 It have a lake"].waitForExistence(timeout: 10), "답변 리포트가 보이지 않는다")
        settle()
        try save("4_answer_report")

        app.tabBars.buttons["기록"].tap()
        XCTAssertTrue(app.staticTexts["최근 회차 IM3 ~ IH"].waitForExistence(timeout: 10), "기록이 보이지 않는다")
        settle()
        try save("5_history")

        app.tabBars.buttons["모의고사"].tap()
        // 모의고사 탭에는 아직 두 리포트가 쌓여 있다.
        let back = app.buttons["BackButton"]
        while back.waitForExistence(timeout: 2) { back.tap() }
        app.tapTag("exam-start")
        app.finishSurvey()
        app.tapTag("exam-choice-3")
        app.tapTag("exam-next-step")
        XCTAssertTrue(app.staticTexts["장비 점검"].waitForExistence(timeout: 30), "장비 점검 화면이 보이지 않는다")
        app.tapTag("exam-play")
        app.waitRecording()
        app.tapTag("exam-next", timeout: 30)
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "문항 1 /")).firstMatch.waitForExistence(timeout: 30))
        app.tapTag("exam-play")
        app.waitRecording()
        Thread.sleep(forTimeInterval: 4)
        try save("2_question")
    }

    /** 등급 구슬과 점수 숫자가 멈출 때까지 기다린다. */
    private func settle() {
        Thread.sleep(forTimeInterval: 2.5)
    }

    private func save(_ name: String) throws {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
        guard let dir = ProcessInfo.processInfo.environment["STORE_SCREENSHOTS_DIR"] else { return }
        try shot.pngRepresentation.write(to: URL(fileURLWithPath: dir).appendingPathComponent("\(name).png"))
    }
}

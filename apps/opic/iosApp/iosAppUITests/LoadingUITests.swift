import XCTest

// 오래 걸리는 버튼이 처리 중 표시를 보이고, 키 확인 중에는 뒤로 나가거나 탭을 옮길 수 없는지 화면 터치로 본다(#133, #152).
// 가짜 Gemini 가 3초 늦게 답해서 키 확인이 3초 걸린다.
final class LoadingUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini + UITestLaunch.slowGemini
        app.launch()
    }

    func testSlowButtonsShowBusyAndKeyCheckBlocksBack() {
        // 첫 실행 키 입력: 제공사가 하나뿐이라 고르지 않아도 저장 버튼이 켜진다.
        let field = app.tag("key-field")
        XCTAssertTrue(field.waitForExistence(timeout: 30), "키 입력 화면이 보이지 않는다")
        field.tap()
        app.typeText("test-key")
        app.tapTag("key-submit")
        assertBusy("key-submit", "첫 실행 키 확인")
        app.tapTag("consent-agree", timeout: 30)

        // 설정에서 연 키 입력: 확인하는 동안 뒤로 버튼(가장자리 스와이프 포함)이 숨는다.
        app.tabBars.buttons["설정"].tap()
        let keyRow = app.staticTexts["Gemini"]
        XCTAssertTrue(keyRow.waitForExistence(timeout: 10), "설정의 키 줄이 보이지 않는다")
        keyRow.tap()
        XCTAssertTrue(app.navigationBars.buttons.firstMatch.waitForExistence(timeout: 10), "키 입력 화면에 뒤로 버튼이 없다")
        app.tag("key-field").tap()
        app.typeText("test-key")
        app.tapTag("key-submit")
        assertBusy("key-submit", "설정 키 확인")
        XCTAssertFalse(app.navigationBars.buttons.firstMatch.exists, "키를 확인하는 동안 뒤로 버튼이 보인다")
        // 확인 중에 다른 탭을 눌러도 키 입력 화면에 남는다.
        XCTAssertEqual(app.tag("key-submit").label, "처리 중", "탭을 누르기 전에 키 확인이 끝났다")
        app.tabBars.buttons["모의고사"].tap()
        XCTAssertTrue(app.tabBars.buttons["설정"].isSelected, "키를 확인하는 동안 탭이 바뀌었다")
        XCTAssertTrue(app.tag("key-field").exists, "키를 확인하는 동안 탭을 누르자 키 입력 화면이 사라졌다")
        XCTAssertTrue(waitUntil(10) { !self.app.tag("key-field").exists }, "키 저장 뒤 설정으로 돌아오지 않았다")

        // 모의시험 점검: Play 를 누르면 질문을 읽는 동안 처리 중이다.
        app.tabBars.buttons["모의고사"].tap()
        app.tapTag("exam-start")
        app.finishSurvey()
        app.tapTag("exam-choice-0")
        app.tapTag("exam-next-step")
        XCTAssertTrue(app.staticTexts["장비 점검"].waitForExistence(timeout: 30), "장비 점검 화면이 보이지 않는다")
        app.tapTag("exam-play")
        assertBusy("exam-play", "점검 Play")
        app.waitRecording()
    }

    /** 버튼 글자가 "처리 중" 으로 바뀌었는지 보고 그 화면을 남긴다. */
    private func assertBusy(_ tag: String, _ name: String) {
        let button = app.tag(tag)
        XCTAssertTrue(waitUntil(2) { button.label == "처리 중" }, "\(name): 처리 중 표시가 보이지 않는다(\(button.label))")
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }
}

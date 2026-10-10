import XCTest

// 서베이를 화면 터치로 끝까지 넘긴다(#167). 모의시험 서베이를 내면 수준 고르기가 나오고,
// 설정의 서베이는 같은 흐름에 방금 낸 답이 채워져 있어 바로 저장할 수 있다. 화면마다 스크린샷을 남긴다.
final class SurveyUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = [UITestLaunch.reset, UITestLaunch.fakeGemini]
        app.launch()
    }

    func testExamSurveyThenSettingsSurveyKeepsAnswers() {
        app.passFirstLaunch()
        app.tapTag("home-exam")
        shot("1-질문")
        app.tapTag("survey-option-work-0")
        XCTAssertTrue(app.tag("survey-option-employed-0").waitForExistence(timeout: 5), "고른 보기의 하위 질문이 바로 다음 화면에 나오지 않는다")
        shot("2-하위질문")
        app.tapTag("survey-previous")
        app.finishSurvey()
        XCTAssertTrue(app.staticTexts["난이도 고르기"].waitForExistence(timeout: 30), "서베이를 내고 수준 고르기로 가지 않는다")
        XCTAssertTrue(app.buttons["나가기"].waitForExistence(timeout: 10))
        app.buttons["나가기"].tap()

        // 설정의 서베이: 저장한 답이 채워져 있어 고른 보기를 다시 눌러 넘기고 묶음은 다음만 눌러 저장한다.
        XCTAssertTrue(app.tabBars.buttons["설정"].waitForExistence(timeout: 30))
        app.tabBars.buttons["설정"].tap()
        app.tapTag("settings-survey")
        for option in ["work-3", "student-1", "course-last-3", "housing-0"] { app.tapTag("survey-option-\(option)") }
        shot("3-묶음")
        for _ in 0..<3 { app.tapTag("survey-next") }
        XCTAssertTrue(waitUntil(5) { self.app.tag("survey-total").label == "12" }, "저장한 주제 12개가 채워져 있지 않다")
        shot("4-마지막묶음")
        app.tapTag("survey-next")
        XCTAssertTrue(app.tag("settings-survey").waitForExistence(timeout: 10), "저장하고 설정으로 돌아가지 않는다")
    }

    private func shot(_ name: String) {
        Thread.sleep(forTimeInterval: 0.6)
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}

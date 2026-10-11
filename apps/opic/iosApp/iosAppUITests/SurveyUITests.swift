import XCTest

// 서베이를 화면 터치로 끝까지 넘긴다(#167). 모의고사 서베이를 내면 수준 고르기가 나오고,
// 설정의 서베이는 같은 흐름에 방금 낸 답이 채워져 있어 바로 저장할 수 있다. 화면마다 스크린샷을 남긴다.
final class SurveyUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini
        app.launch()
    }

    func testExamSurveyThenSettingsSurveyKeepsAnswers() {
        app.passFirstLaunch()
        app.tapTag("exam-start")
        shot("1-질문")
        app.tapTag("survey-option-work-0")
        XCTAssertTrue(app.tag("survey-option-employed-0").waitForExistence(timeout: 5), "고른 보기의 하위 질문이 바로 다음 화면에 나오지 않는다")
        shot("2-하위질문")
        app.tapTag("survey-previous")
        app.finishSurvey()
        XCTAssertTrue(app.staticTexts["난이도 고르기"].waitForExistence(timeout: 30), "서베이를 내고 수준 고르기로 가지 않는다")
        // 나가기 X 는 바로 나가지 않고 확인 대화상자를 띄운다. 취소하면 머물고, 나가기를 눌러야 나간다.
        app.tapTag("exam-exit")
        XCTAssertTrue(app.staticTexts["모의고사을 나갈까요?"].waitForExistence(timeout: 5), "나가기 확인 대화상자가 보이지 않는다")
        shot("2a-나가기확인")
        app.tapTag("alert-cancel")
        XCTAssertTrue(app.staticTexts["난이도 고르기"].waitForExistence(timeout: 5), "취소했는데 시험에 머물지 않는다")
        app.tapTag("exam-exit")
        app.tapTag("alert-confirm")

        // 설정의 서베이: 저장한 답이 채워져 있어 고른 보기를 다시 눌러 넘기고 묶음은 다음만 눌러 저장한다.
        XCTAssertTrue(app.tabBars.buttons["설정"].waitForExistence(timeout: 30))
        app.tabBars.buttons["설정"].tap()
        app.tapTag("settings-survey")
        for option in ["work-3", "student-1", "course-last-3", "housing-0"] { app.tapTag("survey-option-\(option)") }
        XCTAssertTrue(waitUntil(5) { self.app.tag("survey-total").label == "12" }, "첫 묶음 화면에 전체 선택 수가 보이지 않는다")
        shot("3-묶음")
        for _ in 0..<3 { app.tapTag("survey-next") }
        XCTAssertTrue(waitUntil(5) { self.app.tag("survey-total").label == "12" }, "저장한 주제 12개가 채워져 있지 않다")
        shot("4-마지막묶음")
        app.tapTag("survey-next")
        XCTAssertTrue(app.tag("settings-survey").waitForExistence(timeout: 10), "저장하고 설정으로 돌아가지 않는다")

        // 바꾼 답이 있을 때 X 를 누르면 버려진다고 묻고, 나가기를 누르면 저장하지 않고 설정으로 돌아간다.
        app.tapTag("settings-survey")
        app.tapTag("survey-option-work-0")
        app.tapTag("survey-exit")
        XCTAssertTrue(app.staticTexts["서베이를 나갈까요?"].waitForExistence(timeout: 5), "바꾼 답이 있는데 나가기 전에 묻지 않는다")
        shot("5-서베이나가기확인")
        app.tapTag("alert-confirm")
        XCTAssertTrue(app.tag("settings-survey").waitForExistence(timeout: 10), "나가기를 눌렀는데 설정으로 돌아가지 않는다")
    }

    private func shot(_ name: String) {
        Thread.sleep(forTimeInterval: 0.6)
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}

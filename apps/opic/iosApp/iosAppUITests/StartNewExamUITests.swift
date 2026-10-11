import XCTest

// 진행 중인 회차가 있을 때 새로 시작을 누르면 확인 대화상자가 뜨고(#223), 취소하면 이어 보기가 그대로 남고,
// 새로 시작을 누르면 앞 회차가 미완료로 끝나 기록 탭에 "미완료"로 보이는지 화면 터치로 본다.
final class StartNewExamUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini
        app.launch()
    }

    func testStartNewAsksThenEndsPreviousAsIncomplete() {
        app.passFirstLaunch()
        app.tapTag("exam-start")
        app.finishSurvey()
        XCTAssertTrue(app.staticTexts["난이도 고르기"].waitForExistence(timeout: 30), "서베이를 내고 수준 고르기로 가지 않는다")
        app.tapTag("exam-exit")
        app.tapTag("alert-confirm")
        XCTAssertTrue(app.tag("exam-resume").waitForExistence(timeout: 30), "나간 뒤 이어 보기가 보이지 않는다")

        // 취소하면 아무 데도 가지 않고 이어 보기가 남는다.
        app.tapTag("exam-start")
        XCTAssertTrue(app.staticTexts["새 모의고사를 시작할까요?"].waitForExistence(timeout: 5), "새로 시작 확인 대화상자가 보이지 않는다")
        shot("1-새로시작확인")
        app.tapTag("alert-cancel")
        XCTAssertTrue(app.tag("exam-resume").waitForExistence(timeout: 5), "취소했는데 이어 보기가 사라졌다")

        // 새로 시작하면 새 회차 서베이가 열린다.
        app.tapTag("exam-start")
        app.tapTag("alert-confirm")
        XCTAssertTrue(app.tag("exam-exit").waitForExistence(timeout: 30), "새로 시작했는데 시험 화면이 열리지 않는다")
        shot("2-새회차")
        app.tapTag("exam-exit")
        app.tapTag("alert-confirm")

        // 앞 회차는 기록에 미완료로 남는다.
        XCTAssertTrue(app.tabBars.buttons["기록"].waitForExistence(timeout: 30))
        app.tabBars.buttons["기록"].tap()
        let incomplete = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "미완료")).firstMatch
        XCTAssertTrue(incomplete.waitForExistence(timeout: 10), "앞 회차가 기록에 미완료로 보이지 않는다")
        shot("3-기록미완료")
    }

    private func shot(_ name: String) {
        Thread.sleep(forTimeInterval: 0.6)
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}

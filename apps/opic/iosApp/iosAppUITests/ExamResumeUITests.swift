import XCTest

// 모의시험 앞부분을 화면 터치로 돌고, 첫 문항 녹음 중에 앱을 끝낸 뒤 다시 켜서 모의고사 탭의 이어 보기로 같은 문항에 돌아오는지 본다
// (F1.0, F1.1, F1.3, F2.1, F2.3, F3.3). 첫 실행은 저장된 것을 비우고 가짜 Gemini 로 띄워서 가짜 키로 키 확인을 넘기고 출제는 내장 문항으로 간다.
// 녹음 내용은 보지 않는다. 시뮬레이터 마이크는 스킴의 테스트 사전 작업이 미리 허용한다.
final class ExamResumeUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
    }

    func testExamResumesAtSameQuestionAfterTerminate() {
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini
        app.launch()
        app.passFirstLaunch()

        app.tapTag("exam-start")
        app.finishSurvey()
        app.tapTag("exam-choice-0")
        app.tapTag("exam-next-step")

        // 장비 점검: 질문을 듣고 녹음 10초가 지나야 Next 가 켜진다.
        XCTAssertTrue(app.staticTexts["장비 점검"].waitForExistence(timeout: 30), "장비 점검 화면이 보이지 않는다")
        app.tapTag("exam-play")
        app.waitRecording()
        app.tapTag("exam-next", timeout: 60)

        let first = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "문항 1 /")).firstMatch
        XCTAssertTrue(first.waitForExistence(timeout: 30), "점검 뒤 첫 문항이 보이지 않는다")
        let title = first.label
        app.tapTag("exam-play")
        app.waitRecording()
        attachScreenshot("첫 문항 녹음 중")

        // 녹음 중에 앱을 끝내고 다시 켠다. 저장된 것은 그대로 둔다.
        app.terminate()
        app.launchArguments = UITestLaunch.fakeGemini
        app.launch()

        let resume = app.tag("exam-resume")
        XCTAssertTrue(resume.waitForExistence(timeout: 30), "다시 켠 뒤 모의고사 탭에 이어 보기가 보이지 않는다")
        XCTAssertEqual(resume.label, "이어 보기")
        app.tapTag("exam-resume")
        XCTAssertTrue(app.staticTexts[title].waitForExistence(timeout: 30), "이어 본 문항이 \(title) 이 아니다")
        app.tapTag("exam-play")
        app.waitRecording()
        attachScreenshot("이어 본 첫 문항 녹음 중")
        // 끊긴 문항을 다시 들은 것으로 세어 다시 듣기 버튼이 없다(F3.3).
        XCTAssertFalse(app.tag("exam-replay").exists, "이어 본 문항에서 다시 듣기가 보인다")
    }

    private func attachScreenshot(_ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }
}

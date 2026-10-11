import XCTest

// 분석이 실패하면 끝없이 기다리지 않고 이유와 다시 분석하기 버튼이 보이는지 화면 터치로 본다(#203).
// 가짜 Gemini 는 늘 "OK" 를 돌려줘서 분석 응답 검사를 통과하지 못한다. 실제로 응답이 깨지거나 끊긴 것과 같은 길이다.
final class AnalysisFailureUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
    }

    private func launch(_ extra: [String] = []) {
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini + extra
        app.launch()
        app.passFirstLaunch()
    }

    /** 한 문항을 녹음하고 Next 를 눌러 답변 리포트까지 간다. */
    private func answerOneQuestion() {
        tapWhenReady(app.buttons["한 문항 풀기"])
        tapWhenReady(app.buttons["Play"])
        let next = app.buttons["Next"]
        XCTAssertTrue(waitUntil(60) { next.exists && next.isEnabled }, "Next 가 60초 안에 켜지지 않았다")
        next.tap()
        XCTAssertTrue(app.staticTexts["답변 리포트"].waitForExistence(timeout: 10), "Next 뒤 답변 리포트가 보이지 않는다")
    }

    // 사용자가 겪은 경우다. 분석 요청이 연결 오류로 한 번 끊기면 이유와 다시 분석하기가 보이고,
    // 대기열이 30초 뒤 다시 보내 결과를 받으면 누르지 않아도 분석 결과로 바뀐다.
    func testConnectionDropShowsReasonThenResult() {
        launch(UITestLaunch.dropOnce)
        answerOneQuestion()

        XCTAssertTrue(app.tag("answer-retry").waitForExistence(timeout: 30), "연결이 끊겼는데 다시 분석하기가 보이지 않는다")
        XCTAssertTrue(label(containing: "연결이 끊겼거나").exists, "연결 오류 이유가 보이지 않는다")
        attach("연결 끊김 뒤 실패 화면")

        let transcript = label(containing: "I live in Seoul.")
        XCTAssertTrue(transcript.waitForExistence(timeout: 90), "다시 보낸 분석 결과로 바뀌지 않았다")
        XCTAssertFalse(app.tag("answer-retry").exists, "결과가 나왔는데 다시 분석하기가 남아 있다")
        attach("다시 보낸 뒤 분석 결과")
    }

    // 연결이 끝내 돌아오지 않으면 대기열이 세 번 더 보내고(30, 60, 120초 뒤) 멈춘다. 그 뒤에도 기다림이 아니라
    // 연결 오류 이유와 다시 분석하기가 남아야 한다. 다시 누르면 기다림으로 돌아갔다가 다시 실패로 온다.
    func testConnectionNeverBackShowsReasonAndRetry() {
        launch(UITestLaunch.offline)
        answerOneQuestion()

        let retry = app.tag("answer-retry")
        XCTAssertTrue(retry.waitForExistence(timeout: 30), "연결이 끊겼는데 다시 분석하기가 보이지 않는다")
        XCTAssertTrue(label(containing: "연결이 끊겼거나").exists, "연결 오류 이유가 보이지 않는다")

        // 다시 보내기 세 번(모두 210초)이 끝날 때까지 실패 화면이 그대로인지 본다.
        XCTAssertFalse(waitUntil(225) { !retry.exists }, "다시 보내는 동안 실패 화면이 사라졌다")
        XCTAssertTrue(label(containing: "연결이 끊겼거나").exists, "다시 보내기를 다 쓴 뒤 연결 오류 이유가 보이지 않는다")
        attach("다시 보내기를 다 쓴 뒤")

        app.tapTag("answer-retry")
        XCTAssertTrue(retry.waitForExistence(timeout: 30), "다시 분석한 뒤 실패 화면으로 돌아오지 않았다")
        XCTAssertTrue(label(containing: "연결이 끊겼거나").exists, "다시 분석한 뒤 연결 오류 이유가 보이지 않는다")
    }

    func testPracticeAnalysisFailureShowsRetry() {
        launch()
        answerOneQuestion()
        let retry = app.tag("answer-retry")
        XCTAssertTrue(retry.waitForExistence(timeout: 30), "분석이 실패했는데 다시 분석하기 버튼이 보이지 않는다")
        XCTAssertTrue(label(containing: "분석하지 못했습니다").exists, "실패 이유가 보이지 않는다")
        attach("답변 분석 실패")

        // 다시 보내도 가짜 응답이라 또 실패하지만, 다시 보냈다가 실패로 돌아오는지 본다.
        app.tapTag("answer-retry")
        XCTAssertTrue(retry.waitForExistence(timeout: 30), "다시 분석한 뒤 실패 화면으로 돌아오지 않았다")
    }

    func testExamReportAnalysisFailureShowsRetry() {
        launch()
        app.tapTag("home-exam")
        app.finishSurvey()
        app.tapTag("exam-choice-0")
        app.tapTag("exam-next-step")
        for _ in 0..<30 {
            if app.staticTexts["회차 리포트"].exists { break }
            if app.staticTexts["난이도 재조정"].exists {
                app.tapTag("exam-choice-1")
                app.tapTag("exam-next-step")
                continue
            }
            app.tapTag("exam-play", timeout: 60)
            app.waitRecording()
            let next = app.tag("exam-next")
            XCTAssertTrue(waitUntil(20) { next.isEnabled }, "녹음 10초 뒤에도 Next 가 켜지지 않는다")
            app.tapTag("exam-next")
            _ = waitUntil(5) { self.app.staticTexts["회차 리포트"].exists || self.app.tag("exam-play").exists }
        }
        XCTAssertTrue(app.staticTexts["회차 리포트"].waitForExistence(timeout: 15), "회차 리포트로 넘어가지 않았다")
        XCTAssertTrue(app.tag("session-retry").waitForExistence(timeout: 60), "답변 분석이 실패했는데 다시 분석하기 버튼이 보이지 않는다")
        XCTAssertTrue(label(containing: "분석하지 못했습니다").exists, "실패한 답변 수가 보이지 않는다")
        attach("회차 리포트 분석 실패")
    }

    private func tapWhenReady(_ element: XCUIElement) {
        XCTAssertTrue(waitUntil(20) { element.exists && element.isHittable && element.isEnabled }, "\(element) 를 누를 수 없다")
        element.tap()
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

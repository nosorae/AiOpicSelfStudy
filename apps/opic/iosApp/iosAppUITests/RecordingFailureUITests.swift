import XCTest

// 녹음 조각을 쓰지 못했을 때 연습과 시험 화면이 이유와 다시 녹음 버튼을 보이는지 화면 터치로 본다(#152).
// 녹음은 실제로 하되 멈출 때마다 조각 쓰기에 실패했다고 알리는 실행 인자로 띄운다.
final class RecordingFailureUITests: XCTestCase {
    private let app = XCUIApplication()
    private let message = "녹음을 저장하지 못했습니다. 기기 저장 공간을 확인하고 다시 녹음을 누르세요."

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = [UITestLaunch.reset, UITestLaunch.fakeGemini, UITestLaunch.recordFail]
        app.launch()
    }

    func testWriteFailureShowsReasonAndRetry() {
        app.passFirstLaunch()

        // 한 문항 연습: Next 를 눌러도 리포트로 가지 않고 이유와 다시 녹음이 보인다.
        tapWhenReady(app.buttons["한 문항 풀기"])
        tapWhenReady(app.buttons["Play"])
        tapNext()
        XCTAssertTrue(app.staticTexts[message].waitForExistence(timeout: 10), "연습: 녹음 실패 안내가 보이지 않는다")
        XCTAssertFalse(app.staticTexts["답변 리포트"].exists, "연습: 녹음을 못 썼는데 리포트로 갔다")
        attachScreenshot("연습 녹음 실패")
        tapWhenReady(app.buttons["다시 녹음"])
        XCTAssertTrue(waitUntil(60) { self.app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH '녹음 중'")).firstMatch.exists }, "연습: 다시 녹음이 시작되지 않았다")

        // 모의시험 장비 점검: 같은 단계에 남아 이유와 다시 녹음이 보인다.
        tapWhenReady(app.buttons["나가기"])
        app.tapTag("home-exam")
        app.swipeUp()
        app.swipeUp()
        for topic in 0..<12 { app.tapTag("survey-topic-\(topic)") }
        app.tapTag("exam-next-step")
        app.tapTag("exam-choice-0")
        app.tapTag("exam-next-step")
        XCTAssertTrue(app.staticTexts["장비 점검"].waitForExistence(timeout: 30), "장비 점검 화면이 보이지 않는다")
        app.tapTag("exam-play")
        app.waitRecording()
        tapNext()
        XCTAssertTrue(app.staticTexts[message].waitForExistence(timeout: 10), "시험: 녹음 실패 안내가 보이지 않는다")
        XCTAssertTrue(app.staticTexts["장비 점검"].exists, "시험: 녹음을 못 썼는데 다음 단계로 갔다")
        XCTAssertEqual(app.tag("exam-play").label, "다시 녹음")
        attachScreenshot("시험 녹음 실패")
    }

    /** 질문 읽기와 녹음 10초가 지나 Next 가 켜지면 누른다. */
    private func tapNext() {
        let next = app.buttons["Next"]
        XCTAssertTrue(waitUntil(60) { next.exists && next.isEnabled }, "Next 가 60초 안에 켜지지 않았다")
        next.tap()
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

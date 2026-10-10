import XCTest

// 시험 화면의 움직임을 시뮬레이터에서 터치로 확인한다(#158 묶음 5).
// 수준을 두 번 바꿔 고른 뒤 장비 점검과 1번 문항에서 녹음한다. Next 는 녹음 10초 전에는 꺼져 있다가 켜지고,
// 녹음 중 화면은 장면마다 첨부로 남겨 목소리 띠, 문항 진행 점, 남은 시간 원을 눈으로 본다.
final class ExamMotionUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini
        app.launch()
    }

    func testRecordingShowsVoiceAndNextFill() {
        app.passFirstLaunch()
        app.tapTag("home-exam")
        app.finishSurvey()
        app.tapTag("exam-choice-4")
        app.tapTag("exam-choice-1")
        snapshot("수준 선택")
        app.tapTag("exam-next-step")
        XCTAssertTrue(app.staticTexts["장비 점검"].waitForExistence(timeout: 30), "장비 점검 화면이 보이지 않는다")

        recordUntilNext("장비 점검 녹음 중")
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "문항 1 /")).firstMatch.waitForExistence(timeout: 30))
        recordUntilNext("1번 녹음 중")
    }

    /** Play 를 눌러 녹음이 시작되면 Next 가 꺼져 있는 것을 보고, 켜질 때까지 기다려 누른다. */
    private func recordUntilNext(_ name: String) {
        app.tapTag("exam-play")
        app.waitRecording()
        let next = app.tag("exam-next")
        XCTAssertFalse(next.isEnabled, "녹음 시작 직후 Next 가 켜져 있다")
        Thread.sleep(forTimeInterval: 4)
        snapshot(name)
        XCTAssertTrue(waitUntil(15) { next.isEnabled }, "녹음 10초 뒤에도 Next 가 켜지지 않는다")
        app.tapTag("exam-next")
    }

    private func snapshot(_ name: String) {
        let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }
}

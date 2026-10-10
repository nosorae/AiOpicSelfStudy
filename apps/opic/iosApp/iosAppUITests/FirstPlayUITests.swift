import XCTest

// 출제 요청이 Play 를 붙잡지 않는지 화면 터치로 잰다(#150). 가짜 Gemini 가 3초 늦게 답한다.
// 점검과 1번은 내장 문항이라 바로 읽고, 2~7번은 1번 Play 때 뒤에서 요청해서 1번 답(10초 넘게)을 하는 동안 받는다.
// 세 Play 모두 질문을 만들고 있다는 안내가 뜨지 않아야 한다. Play 에서 녹음 시작까지 걸린 시간을 남긴다. 기기 음성이 질문을 읽는 시간이 들어 있다.
final class FirstPlayUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = [UITestLaunch.reset, UITestLaunch.fakeGemini, UITestLaunch.slowGemini]
        app.launch()
    }

    func testFirstPlayDoesNotWaitForGemini() {
        app.passFirstLaunch()
        app.tapTag("home-exam")
        app.swipeUp()
        app.swipeUp()
        for topic in 0..<12 { app.tapTag("survey-topic-\(topic)") }
        app.tapTag("exam-next-step")
        app.tapTag("exam-choice-0")
        app.tapTag("exam-next-step")
        XCTAssertTrue(app.staticTexts["장비 점검"].waitForExistence(timeout: 30), "장비 점검 화면이 보이지 않는다")

        let check = playSeconds("점검")
        app.tapTag("exam-next", timeout: 30)
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "문항 1 /")).firstMatch.waitForExistence(timeout: 30))
        let first = playSeconds("1번")
        app.tapTag("exam-next", timeout: 30)
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "문항 2 /")).firstMatch.waitForExistence(timeout: 30))
        let second = playSeconds("2번")

        print("FIRST_PLAY_SECONDS check=\(check) q1=\(first) q2=\(second)")
        XCTAssertFalse(sawLoading, "Play 가 질문을 불러오느라 기다렸다")
    }

    private var sawLoading = false

    /** Play 를 눌러 녹음이 시작되기까지의 초. 그동안 불러오는 중 안내가 보였으면 [sawLoading] 을 켠다. */
    private func playSeconds(_ name: String) -> Double {
        let start = Date()
        app.tapTag("exam-play")
        let phase = app.tag("exam-phase")
        let loading = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "질문을 만들고 있어요")).firstMatch
        XCTAssertTrue(waitUntil(60) {
            if loading.exists { self.sawLoading = true }
            return phase.exists && phase.label.hasPrefix("녹음 중")
        }, "녹음 시작 표시가 보이지 않는다")
        let seconds = Date().timeIntervalSince(start)
        let note = XCTAttachment(string: "\(name) Play 에서 녹음 시작까지 \(String(format: "%.1f", seconds))초")
        note.name = name
        note.lifetime = .keepAlways
        add(note)
        return seconds
    }
}

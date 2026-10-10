import XCTest

// 질문 음성을 화면 터치로 바꿔 가며 듣는다(#161). 점검 질문은 내장 문항이라 앱에 넣은 Gemini 음성 파일을 튼다.
// 설정에서 기기 음성으로 바꾸면 1번 질문은 기기 음성으로 읽는다. 무엇으로 읽었는지는 앱이 기기 로그에 `IosTts:` 로 남긴다.
// 점검 질문은 녹음 시작 5초 안에 다시 듣기를 눌러 같은 음성 파일을 한 번 더 튼다(기기 로그에 `IosTts: 음성 파일을 튼다` 가 두 번).
// 귀로 듣는 판정은 하지 않는다. 모든 경우 질문을 끝까지 읽어 녹음이 저절로 시작되는지만 본다.
final class QuestionVoiceUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
    }

    func testBundledVoiceThenDeviceVoice() {
        app.launchArguments = [UITestLaunch.reset, UITestLaunch.fakeGemini]
        app.launch()
        app.passFirstLaunch()

        app.tabBars.buttons["설정"].tap()
        XCTAssertTrue(app.tag("settings-voice-natural").waitForExistence(timeout: 10), "설정에 질문 음성이 보이지 않는다")
        app.tabBars.buttons["홈"].tap()

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
        app.tapTag("exam-replay", timeout: 4)
        app.waitRecording()
        app.tapTag("exam-next", timeout: 60)
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "문항 1 /")).firstMatch.waitForExistence(timeout: 30))

        // 1번 Play 전에 앱을 끝내고 설정에서 기기 음성으로 바꾼 뒤 이어 본다.
        app.terminate()
        app.launchArguments = [UITestLaunch.fakeGemini]
        app.launch()
        app.tabBars.buttons["설정"].tap()
        app.tapTag("settings-voice-device")
        attach("설정 질문 음성")
        app.tabBars.buttons["홈"].tap()
        app.tapTag("home-exam")
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "문항 1 /")).firstMatch.waitForExistence(timeout: 30))
        app.tapTag("exam-play")
        app.waitRecording()
    }

    private func attach(_ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }
}

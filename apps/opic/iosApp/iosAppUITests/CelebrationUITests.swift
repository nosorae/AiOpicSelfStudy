import XCTest

// 축하가 뜨는지 시뮬레이터에서 화면 터치로 본다(#158 묶음 6).
// 키를 저장하면 체크 둘레에 작은 축하, 모의시험을 끝까지 답하면 리포트로 가기 전에 큰 축하와 "다 마쳤어요" 문구가 보인다.
// 축하가 터지는 동안 장면을 여러 장 첨부로 남겨 움직임을 눈으로 본다.
final class CelebrationUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = UITestLaunch.reset + UITestLaunch.fakeGemini
        app.launch()
    }

    func testKeySavedShowsSmallCelebration() {
        let field = app.tag("key-field")
        XCTAssertTrue(field.waitForExistence(timeout: 30), "키 입력 화면이 보이지 않는다")
        app.tapTag("key-provider-gemini")
        field.tap()
        app.typeText("test-key")
        app.tapTag("key-submit")

        // 체크와 축하는 넘어가기 전 1.2초만 보여서 기다리지 않고 바로 장면을 이어 남긴다.
        burst("키 저장 축하", count: 8)
        XCTAssertTrue(app.tag("consent-agree").waitForExistence(timeout: 10), "축하 뒤 동의 화면으로 넘어가지 않았다")
    }

    func testFinishingExamShowsFanfare() {
        app.passFirstLaunch()
        app.tapTag("exam-start")
        app.finishSurvey()
        // 수준 1 이 문항이 가장 적다.
        app.tapTag("exam-choice-0")
        app.tapTag("exam-next-step")

        let finished = label(containing: "문항을 다 마쳤어요")
        var seen = false
        for _ in 0..<30 {
            if app.staticTexts["난이도 재조정"].exists {
                app.tapTag("exam-choice-1")
                app.tapTag("exam-next-step")
                continue
            }
            let last = app.staticTexts.matching(NSPredicate(format: "label MATCHES %@", "문항 ([0-9]+) / \\1")).firstMatch.exists
            answerOne()
            // 다 마친 화면은 리포트로 가기 전 2초만 보인다. 마지막 문항이면 축하가 터지는 동안 장면부터 남기고 문구를 찾는다.
            if last {
                burst("시험 끝 축하", count: 5)
                seen = finished.waitForExistence(timeout: 2)
                break
            }
        }
        XCTAssertTrue(seen, "마지막 문항 뒤 다 마친 축하 화면이 보이지 않는다")
        XCTAssertTrue(app.staticTexts["회차 리포트"].waitForExistence(timeout: 15), "축하 뒤 회차 리포트로 넘어가지 않았다")
    }

    /** Play 를 눌러 녹음이 시작되면 Next 가 켜질 때까지 기다려 누른다. */
    private func answerOne() {
        app.tapTag("exam-play", timeout: 60)
        app.waitRecording()
        let next = app.tag("exam-next")
        XCTAssertTrue(waitUntil(20) { next.isEnabled }, "녹음 10초 뒤에도 Next 가 켜지지 않는다")
        app.tapTag("exam-next")
    }

    private func label(containing text: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
    }

    /** 움직이는 동안 장면을 [count] 장 이어서 남긴다. 첨부 이름 끝의 번호 순서가 시간 순서다. */
    private func burst(_ name: String, count: Int) {
        for index in 0..<count {
            let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            shot.name = "\(name) \(index)"
            shot.lifetime = .keepAlways
            add(shot)
        }
    }
}

import XCTest

final class ChromeUITests: XCTestCase {
    private func snap(_ name: String) {
        let a = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        a.name = name
        a.lifetime = .keepAlways
        add(a)
    }

    private func el(_ app: XCUIApplication, _ prefix: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
    }

    func testTabStateEdgeSwipeAndExamHidesTabs() throws {
        let app = XCUIApplication()
        app.launch()

        // 1. 탭 전환 뒤 Compose 상태(rememberSaveable 숫자, remember 화면 번호)가 남는지
        let counter = el(app, "화면 #")
        if !counter.waitForExistence(timeout: 10) { print(app.debugDescription); XCTFail("no counter"); return }
        counter.tap(); counter.tap(); counter.tap()
        XCTAssertEqual(counter.label, "화면 #1, 누른 횟수 3")
        app.tabBars.buttons["기록"].tap()
        sleep(1)
        app.tabBars.buttons["홈"].tap()
        sleep(1)
        let again = el(app, "화면 #")
        XCTAssertEqual(again.label, "화면 #1, 누른 횟수 3")
        snap("tab-state")

        // 2. 하위 화면: iOS 는 탭을 남긴다
        el(app, "리포트 1").tap()
        XCTAssertTrue(app.navigationBars["리포트 상세"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.tabBars.firstMatch.isHittable)

        // 3. 왼쪽 가장자리 스와이프 뒤로가기
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.0, dy: 0.5))
        let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5))
        start.press(forDuration: 0.05, thenDragTo: end, withVelocity: .slow, thenHoldForDuration: 0.1)
        sleep(1)
        XCTAssertFalse(app.navigationBars["리포트 상세"].exists)
        XCTAssertTrue(again.exists)
        snap("after-edge-swipe")

        // 4. 시험 화면: 탭 바 없음
        el(app, "리포트 1").tap()
        XCTAssertTrue(app.navigationBars["리포트 상세"].waitForExistence(timeout: 5))
        el(app, "시험 시작").tap()
        sleep(2)
        XCTAssertTrue(el(app, "시험 진행").exists)
        XCTAssertFalse(app.tabBars.firstMatch.isHittable)
        snap("exam")
    }
}

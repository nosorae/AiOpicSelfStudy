import XCTest

/**
 * 앱이 읽는 실행 인자. reset 은 Kotlin 의 `UiTestLaunch` 와 같다. 나머지는 디버깅 스위치(`DebugSwitches`)를 켠다.
 * 스위치는 NSUserDefaults 의 `debug_tools.<key>` 라서 `-key YES` 실행 인자로 켜진다. 테스트는 Debug 구성이라 실제 디버깅 모듈이 읽는다.
 */
enum UITestLaunch {
    /** 저장된 키, 설정, DB, 녹음을 지우고 첫 실행처럼 시작한다. */
    static let reset = ["ui-test-reset"]
    /** Gemini 대신 늘 "OK" 를 돌려주는 가짜를 쓴다. 가짜 키가 키 확인을 넘고, 출제는 내장 문항으로 간다. */
    static let fakeGemini = ["-debug_tools.fake_gemini", "YES"]
    /** Gemini 요청을 3초 늦게 보낸다. 처리 중 표시를 볼 때 쓴다. */
    static let slowGemini = ["-debug_tools.slow_response", "YES"]
    /** 녹음은 실제로 하되 멈출 때마다 조각 쓰기에 실패했다고 알린다. */
    static let recordFail = ["-debug_tools.record_fail", "YES"]
    /** 가짜 Gemini 가 첫 분석 요청만 연결 오류로 끊고 그 뒤로는 분석 결과를 준다. 연결이 잠깐 끊겼다 돌아온 경우다. */
    static let dropOnce = ["-debug_tools.drop_once", "YES"]
    /** 가짜 Gemini 가 분석 요청을 모두 연결 오류로 끊는다. 연결이 돌아오지 않는 경우다. */
    static let offline = ["-debug_tools.offline", "YES"]
    /** 녹음을 실은 요청(분석, 등급)이 분석 작업의 시간 제한(2분)보다 오래 멈춘다. */
    static let stall = ["-debug_tools.stall", "YES"]
    /** 녹음 분석 요청만 시스템 백그라운드 전송으로 이 Mac 의 테스트 서버(127.0.0.1:8765)에 보낸다. */
    static let localUpload = ["-debug_tools.local_upload", "YES"]
}

extension XCUIApplication {
    /** Compose testTag 는 iOS 접근성 식별자로 나온다. */
    func tag(_ id: String) -> XCUIElement {
        descendants(matching: .any).matching(identifier: id).firstMatch
    }

    /** 화면 밖이면 위로 밀어 올린다. 스크롤이 멈춰 위치가 두 번 연달아 같을 때 누른다. 움직이는 중에 누르면 스크롤만 멈추고 눌리지 않는다. */
    func tapTag(_ id: String, timeout: TimeInterval = 20) {
        let element = tag(id)
        let deadline = Date().addingTimeInterval(timeout)
        var last = CGRect.null
        while Date() < deadline {
            if element.exists, element.isHittable, element.isEnabled {
                if element.frame == last {
                    element.tap()
                    return
                }
                last = element.frame
            } else if element.exists, !element.isHittable {
                swipeUp(velocity: .slow)
            }
            Thread.sleep(forTimeInterval: 0.3)
        }
        XCTFail("\(id) 를 \(Int(timeout))초 안에 누를 수 없다")
    }

    /** 첫 실행이면 가짜 키를 넣고 음성 전송에 동의한다. 홈이 보이면 그대로 둔다. */
    func passFirstLaunch() {
        let field = tag("key-field")
        let home = tag("home-exam")
        XCTAssertTrue(waitUntil(30) { field.exists || home.exists }, "키 입력과 홈 둘 다 보이지 않는다")
        guard field.exists else { return }
        tapTag("key-provider-gemini")
        field.tap()
        typeText("test-key")
        tapTag("key-submit")
        tapTag("consent-agree", timeout: 30)
    }

    /**
     * 서베이를 끝까지 넘긴다. Part 1~3 은 하위 질문이 가장 적은 길(일한 적 없음, 학생 아님, 수업 5년 넘음, 혼자)로 고르고,
     * 네 묶음에서 묶음 최소를 넘게 합쳐 12개를 고른다. 하나만 고르는 질문은 고르면 바로 다음 화면이다.
     */
    func finishSurvey() {
        for option in ["work-3", "student-1", "course-last-3", "housing-0"] { tapTag("survey-option-\(option)") }
        let groups = [
            ["movies", "performances", "concerts", "museums", "parks"],
            ["music", "instrument", "singing"],
            ["basketball", "baseball", "soccer"],
            ["domestic-travel"],
        ]
        for (index, topics) in groups.enumerated() {
            for topic in topics { tapTag("survey-topic-\(topic)") }
            if index == groups.count - 1 {
                XCTAssertTrue(waitUntil(5) { self.tag("survey-total").label == "12" }, "전체 선택 수 12 가 보이지 않는다")
            }
            tapTag("survey-next")
        }
    }

    /** 녹음 시작 표시("녹음 중")가 보일 때까지 기다린다. 녹음 내용은 보지 않는다. */
    func waitRecording() {
        let phase = tag("exam-phase")
        XCTAssertTrue(waitUntil(60) { phase.exists && phase.label.hasPrefix("녹음 중") }, "녹음 시작 표시가 보이지 않는다")
    }
}

func waitUntil(_ seconds: TimeInterval, _ condition: @escaping () -> Bool) -> Bool {
    let predicate = NSPredicate { _, _ in condition() }
    return XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: predicate, object: nil)], timeout: seconds) == .completed
}

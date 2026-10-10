# debug

앱 안에서 로그, 네트워크 호출, 기능 스위치, 앱 상태, 크래시 직전 기록을 한 화면에서 보고 텍스트 파일 하나로 공유하는 디버깅 도구다. 이 앱에 묶이지 않게 프로젝트의 다른 모듈을 의존하지 않는다. 릴리스 빌드는 공개 API 가 같은 빈 모듈 [`libs/debug-noop`](../debug-noop/README.md) 으로 바꿔 끼워 기록도 화면도 남지 않는다.

## 쓰는 법

앱 코드는 `DebugTools` 하나만 쓴다. 컴파일은 `debug-noop` 으로 하고, 디버그 빌드의 실행 파일을 만들 때만 이 모듈로 바꾼다.

```kotlin
// Android: Application.onCreate, iOS: 앱 시작 때 한 번
DebugTools.install(application)            // iOS 는 DebugTools.install()

DebugTools.log(DebugLevel.Warn, "Key", "키 확인 실패: $status", error)

val slow = DebugTools.switch("slow_response", "느린 응답")
if (slow.enabled) delay(3.seconds)         // 읽을 때마다 저장된 값을 본다

DebugTools.panel("오디오") { listOf("경로" to audio.route(), "모드" to audio.mode()) }

val http = HttpClient { installDebugTools() }   // io.github.nosorae.aiopic.libs.debug.network
val fake = fakeHttpEngine(json)              // 늘 200 과 json 을 주는 엔진. noop 은 null 이라 릴리스에는 가짜 응답 코드가 없다
DebugTools.open()                          // 앱의 설정 화면 줄이나 UI 테스트에서 연다
if (DebugTools.installed) { /* 디버그 빌드에서만 "개발자 도구" 줄을 보인다. noop 은 늘 false */ }
```

## 여는 법

| 플랫폼 | 방법 |
|---|---|
| Android | 기기를 흔든다(중력의 2.7배, 1초에 한 번). 앱 화면이 앞에 있을 때만 센서를 듣는다. 계속 떠 있는 알림을 눌러도 열린다. Android 13 이상은 화면을 처음 열 때 알림 권한을 묻는다 |
| iOS | 시뮬레이터의 흔들기는 가속도 센서가 아니라 UIKit 이벤트라서 앱 쪽 Swift 에서 받아 넘긴다. 앱이 직접 띄우려면 `DebugTools.viewController()` 를 present 한다 |
| 둘 다 | `DebugTools.open()`. 흔들기를 못 하는 계측 테스트와 XCUITest 는 앱의 메뉴 줄에서 이걸 부른다 |

iOS 흔들기는 앱 Swift 에 다음을 둔다.

```swift
extension UIWindow {
    open override func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
        super.motionEnded(motion, with: event)
        if motion == .motionShake { DebugTools.shared.open() }
    }
}
```

## 화면

| 탭 | 보여 주는 것 |
|---|---|
| Logs | 최근 것부터. 검색(태그, 메시지, 예외)과 단계 필터(고른 단계 이상). 예외는 첫 줄만 보이고 누르면 스택 전체가 펼쳐진다 |
| Network | 메서드, 상태 코드, 걸린 시간, 주소. 실패(400 이상, 응답 없음)는 빨간색. 누르면 헤더와 본문이 펼쳐지고 cURL 로 복사한다 |
| Switches | 줄을 누르면 켜고 끈다. 저장된 값을 바로 다시 읽는다 |
| Panels | 앱이 더한 패널을 연 때 읽는다. Reload 로 다시 읽는다 |
| Info | 앱 id, 버전, OS, 기기, 언어, 남은 저장 공간 |
| Crashes | 지난 크래시 파일. 하나씩 지운다 |

Share 는 보고서 전체를 `debug-report.txt` 로 써서 시스템 공유 시트에 넘긴다. 보고서가 수 MB 가 될 수 있어 문자열 대신 파일로 넘긴다(Android 는 이 모듈의 FileProvider, 권한은 `${applicationId}.debugtools`). 화면은 앱 디자인 체계를 쓰지 않고 Material 기본 테마로 그려 다른 앱에 그대로 붙는다. 시스템 다크 모드를 따른다.

## 하는 일

| 기능 | 동작 |
|---|---|
| 로그 | 메모리에 최근 2,000줄(`DebugConfig.maxLogs`)만 둔다. 넘으면 오래된 것부터 버린다 |
| 가림 | 저장하기 전에 Gemini 키 형식(`AQ.` 뒤 20자 이상, `AIza` 뒤 35자), `x-goog-api-key:` 헤더 값, URL 의 `key`, `api_key`, `access_token` 값을 `***` 로 바꾼다. 앱은 `DebugConfig.redactPatterns` 로 패턴을 더한다. 공유한 파일에도 키가 남지 않는다 |
| 기능 스위치 | Android SharedPreferences, iOS NSUserDefaults 에 저장한다. `enabled` 를 읽을 때마다 저장된 값을 봐서 앱을 다시 시작하지 않아도 적용된다. 저장된 값이 없으면 기본값이다 |
| 앱 패널 | 앱이 "이름, 값" 줄을 돌려주는 함수를 넘긴다. 함수가 실패하면 그 패널에 오류 메시지만 적는다 |
| 기기와 앱 정보 | 앱 id, 버전과 빌드 번호, OS, 기기 모델, 언어, 남은 저장 공간 |
| 크래시 | 처리 안 된 예외가 나면 그 순간의 예외, 스위치, 로그를 파일로 쓰고(최근 5개) 다음 실행의 보고서에 넣는다. 원래 처리기는 그대로 부른다. iOS 의 네이티브 신호 크래시는 잡지 않는다 |
| 네트워크 | Ktor 클라이언트에 `installDebugTools()` 를 걸면 요청마다 주소, 메서드, 상태 코드, 걸린 시간, 헤더, 본문을 남긴다. 최근 200건(`maxCalls`)만 두고 본문 하나는 16KB(`maxBodyBytes`)에서 잘라 원래 크기를 적는다. 헤더 `x-goog-api-key`, `authorization`, `proxy-authorization`, `cookie`, `set-cookie`, `x-api-key` 는 값을 통째로 가리고 앱은 `redactHeaders` 로 이름을 더한다. 실패한 요청도 남기고 예외는 그대로 앱에 넘긴다. 스트리밍 응답은 아직 따로 다루지 않는다 |
| 보고서 | 정보, 스위치, 앱 패널, 크래시, 로그, 네트워크를 텍스트 하나로 묶는다 |

기록은 메모리에만 두고 크래시 때만 파일로 쓴다. 평소에는 디스크에 쓰지 않는다.

## 두 모듈의 API 맞추기

두 모듈 모두 Kotlin ABI 검사를 켜서 공개 API 를 `api/` 덤프로 커밋한다. `./gradlew check` 는 덤프가 코드와 같은지(`checkKotlinAbi`), noop 덤프의 모든 줄이 이 모듈 덤프에 있는지(`checkNoopAbi`)를 본다. 공개 API 를 바꾸면 두 모듈을 같이 고치고 덤프를 다시 쓴다.

```bash
./gradlew :libs:debug:updateKotlinAbi :libs:debug-noop:updateKotlinAbi
```

## 패키지

지금은 저장소 규칙대로 `io.github.nosorae.aiopic.libs.debug` 다. 다른 앱에 배포하기로 정하면 앱 이름이 빠진 패키지로 바꾼다.

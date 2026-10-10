# debug

앱 안에서 로그, 기능 스위치, 앱 상태, 크래시 직전 기록을 모아 텍스트 하나로 보는 디버깅 도구다. 이 앱에 묶이지 않게 프로젝트의 다른 모듈을 의존하지 않는다. 릴리스 빌드는 공개 API 가 같은 빈 모듈 [`libs/debug-noop`](../debug-noop/README.md) 으로 바꿔 끼워 기록도 화면도 남지 않는다.

## 쓰는 법

앱 코드는 `DebugTools` 하나만 쓴다. 컴파일은 `debug-noop` 으로 하고, 디버그 빌드의 실행 파일을 만들 때만 이 모듈로 바꾼다.

```kotlin
// Android: Application.onCreate, iOS: 앱 시작 때 한 번
DebugTools.install(application)            // iOS 는 DebugTools.install()

DebugTools.log(DebugLevel.Warn, "Key", "키 확인 실패: $status", error)

val slow = DebugTools.switch("slow_response", "느린 응답")
if (slow.enabled) delay(3.seconds)         // 읽을 때마다 저장된 값을 본다

DebugTools.panel("오디오") { listOf("경로" to audio.route(), "모드" to audio.mode()) }
```

## 하는 일

| 기능 | 동작 |
|---|---|
| 로그 | 메모리에 최근 2,000줄(`DebugConfig.maxLogs`)만 둔다. 넘으면 오래된 것부터 버린다 |
| 가림 | 저장하기 전에 Gemini 키 형식(`AQ.` 뒤 20자 이상, `AIza` 뒤 35자), `x-goog-api-key:` 헤더 값, URL 의 `key`, `api_key`, `access_token` 값을 `***` 로 바꾼다. 앱은 `DebugConfig.redactPatterns` 로 패턴을 더한다. 공유한 파일에도 키가 남지 않는다 |
| 기능 스위치 | Android SharedPreferences, iOS NSUserDefaults 에 저장한다. `enabled` 를 읽을 때마다 저장된 값을 봐서 앱을 다시 시작하지 않아도 적용된다. 저장된 값이 없으면 기본값이다 |
| 앱 패널 | 앱이 "이름, 값" 줄을 돌려주는 함수를 넘긴다. 함수가 실패하면 그 패널에 오류 메시지만 적는다 |
| 기기와 앱 정보 | 앱 id, 버전과 빌드 번호, OS, 기기 모델, 언어, 남은 저장 공간 |
| 크래시 | 처리 안 된 예외가 나면 그 순간의 예외, 스위치, 로그를 파일로 쓰고(최근 5개) 다음 실행의 보고서에 넣는다. 원래 처리기는 그대로 부른다. iOS 의 네이티브 신호 크래시는 잡지 않는다 |
| 보고서 | 정보, 스위치, 앱 패널, 크래시, 로그를 텍스트 하나로 묶는다 |

기록은 메모리에만 두고 크래시 때만 파일로 쓴다. 평소에는 디스크에 쓰지 않는다.

네트워크 기록(Ktor 플러그인), 화면과 공유, 흔들어 열기는 다음 단계에서 더한다.

## 두 모듈의 API 맞추기

두 모듈 모두 Kotlin ABI 검사를 켜서 공개 API 를 `api/` 덤프로 커밋한다. `./gradlew check` 는 덤프가 코드와 같은지(`checkKotlinAbi`), noop 덤프의 모든 줄이 이 모듈 덤프에 있는지(`checkNoopAbi`)를 본다. 공개 API 를 바꾸면 두 모듈을 같이 고치고 덤프를 다시 쓴다.

```bash
./gradlew :libs:debug:updateKotlinAbi :libs:debug-noop:updateKotlinAbi
```

## 패키지

지금은 저장소 규칙대로 `io.github.nosorae.aiopic.libs.debug` 다. 다른 앱에 배포하기로 정하면 앱 이름이 빠진 패키지로 바꾼다.

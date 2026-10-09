---
paths:
  - "**/src/commonMain/**"
  - "**/src/commonTest/**"
---

# commonMain 규칙

두 플랫폼이 같은 동작을 하는지는 공통 코드 테스트로 보장한다. 그래서 시험 진행 계산, 출제, Gemini 요청과 응답 해석, 답변 저장소, 리포트 집계, 화면은 모두 commonMain 에 둔다. 플랫폼 소스셋에는 녹음, 키 보관, 음성 합성, 백그라운드 작업처럼 플랫폼 API 가 꼭 필요한 것만 둔다.

- `java.*`, `android.*`, `platform.*` 를 import 하지 않는다. 필요하면 commonMain 에 인터페이스를 두고 플랫폼 소스셋에 구현 클래스를 둔다
- `expect class` 대신 인터페이스와 플랫폼 구현 클래스를 쓴다. 테스트에서 Fake 로 바꿔 끼우기 쉽다. `expect fun` 은 Room 생성자처럼 도구가 요구할 때만 쓴다
- Activity 같은 화면이 있어야 되는 기능(마이크 권한 요청 등)은 `@Composable fun rememberXxx()` 로 공통 코드에 노출한다
- 시간은 `Clock`, `TimeSource` 를 주입받는다. `Clock.System` 을 바로 부르지 않는다. 40분 타이머와 녹음 시간을 테스트에서 돌려야 한다
- 디스패처는 주입받는다. `Dispatchers.IO` 를 코드에 박지 않는다
- 시간 길이는 `Long` 밀리초 대신 `Duration` 으로 받는다
- 팩 내장 파일(문항, 프롬프트)은 `composeResources/files` 에 두고 `Res.readBytes` 로 읽는다. 읽는 함수와 문자열을 해석하는 순수 함수를 나누고 순수 함수를 commonTest 에서 검사한다
- `@Preview` 는 commonMain 에 둔다. 상태 없는 화면에 고정 UiState 를 넣어 그린다
- 사용자에게 보이는 문구는 Compose 리소스 문자열로 둔다. 시험에 딸린 문구는 시험 팩이 준다

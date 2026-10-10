# haptics

손에 느껴지는 짧은 진동을 내는 라이브러리다. 공개 API 는 효과 종류 `HapticEffect` 와 재생 함수 `Haptics` 하나, 플랫폼마다 만드는 함수 하나뿐이고 구현은 internal 이다. 프로젝트의 다른 모듈을 의존하지 않는다. 결정 이유는 [ADR 0029](../../docs/adr/0029-진동은-별도-라이브러리로-두고-시스템-설정을-따른다.md).

| 플랫폼 | 만드는 함수 | 구현 |
|---|---|---|
| Android | `androidHaptics(view)` | `View.performHapticFeedback`, 축하만 API 33 이상에서 `VibrationEffect.Composition` |
| iOS | `iosHaptics()` | `UISelectionFeedbackGenerator`, `UIImpactFeedbackGenerator`, `UINotificationFeedbackGenerator`. 메인 스레드에서 낸다 |

```kotlin
val haptics = LocalHaptics.current // ui/design. 앱 틀이 넣고, 기본값은 아무것도 안 한다
haptics.play(HapticEffect.Success)
```

두 플랫폼 모두 기기의 시스템 터치 진동 설정을 따른다. 앱 설정의 "진동"을 끄면 앱 틀이 재생을 막는다.

## 언제 어떤 효과를 쓰나

남발하지 않는다. 아래 순간에만 쓰고, 표에 없는 버튼에는 넣지 않는다.

| 순간 | 효과 | Android | iOS |
|---|---|---|---|
| 칩, 목록, 토글에서 고른 것이 바뀜 | `Selection` | `SEGMENT_TICK` (API 34), 아래는 `CLOCK_TICK` | 선택 진동 |
| 주요 버튼 누름 (시험 시작, 다음 문항, 제출) | `Tap` | `VIRTUAL_KEY` | 가벼운 충격 |
| 녹음 시작 | `RecordStart` | `GESTURE_START` (API 30), 아래는 `LONG_PRESS` | 중간 충격 |
| 녹음 끝 | `RecordStop` | `GESTURE_END` (API 30), 아래는 `CONTEXT_CLICK` | 중간 충격 |
| 키 저장 성공 | `Success` | `CONFIRM` (API 30), 아래는 `VIRTUAL_KEY` | 성공 알림 |
| 되돌릴 수 없는 일 확인 (키 지우기, 동의 철회) | `Warning` | `LONG_PRESS` | 경고 알림 |
| 오류 안내 | `Error` | `REJECT` (API 30), 아래는 `LONG_PRESS` | 오류 알림 |
| 모의시험 완료와 등급 공개 (회차에 한 번) | `Celebrate` | Composition 연속 진동, 못 내면 `Success` 와 같다 | 성공 알림 뒤 가벼운 충격 두 번 |

## 테스트

Android 쪽 상수 고르기와 축하 대체는 Robolectric 호스트 테스트, 모든 효과가 예외 없이 불리는지는 에뮬레이터 계측 테스트(`androidDeviceTest`)와 iOS 시뮬레이터 테스트로 본다. 손 느낌은 실기기에서 사람이 본다.

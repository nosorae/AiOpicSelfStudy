# tts

질문을 기기 음성으로 읽는 라이브러리다. 실제 시험처럼 질문이 끝나면 녹음이 저절로 시작돼야 해서 `speak` 는 다 읽을 때까지 돌아오지 않는다. 고품질 음성이 설치돼 있으면 그것을 고른다. 프로젝트의 다른 모듈을 의존하지 않는다.

| 플랫폼 | 구현 |
|---|---|
| Android | `AndroidTts(context)`, `TextToSpeech` 의 끝 알림을 기다린다 |
| iOS | `IosTts()`, `AVSpeechSynthesizer` 의 끝 알림을 기다린다 |

```kotlin
if (tts.speak(question)) recorder.start(dir) // false 면 멈췄거나 읽지 못했다. 녹음을 시작하지 않는다
tts.stop()
```

코루틴이 취소되면 읽기도 멈춘다. Android 쪽 끝 알림은 Robolectric 호스트 테스트로, iOS 쪽은 시뮬레이터 테스트로 확인한다.

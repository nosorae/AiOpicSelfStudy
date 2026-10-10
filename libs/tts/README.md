# tts

질문을 읽는 라이브러리다. 미리 만든 음성 파일(WAV, m4a)이 있으면 그것을 틀고, 없거나 틀지 못하면 기기 음성으로 읽는다. 실제 시험처럼 질문이 끝나면 녹음이 저절로 시작돼야 해서 `speak` 는 다 읽을 때까지 돌아오지 않는다. 기기 음성은 설치된 영어(미국) 음성 가운데 가장 좋은 것(iOS 는 Premium, Enhanced, 기본 순)을 고른다. 프로젝트의 다른 모듈을 의존하지 않는다.

| 플랫폼 | 구현 |
|---|---|
| Android | `AndroidTts(context)`, 파일은 `MediaPlayer`, 기기 음성은 `TextToSpeech` 의 끝 알림을 기다린다 |
| iOS | `IosTts()`, 파일은 `AVAudioPlayer`, 기기 음성은 `AVSpeechSynthesizer` 의 끝 알림을 기다린다. 둘 다 무음 모드에서도 들리게 녹음 겸용 카테고리로 튼다 |

```kotlin
if (tts.speak(question, audio = voiceFile)) recorder.start(dir) // false 면 멈췄거나 읽지 못했다. 녹음을 시작하지 않는다
tts.stop()
tts.hasHighQualityVoice() // 기본보다 좋은 기기 음성이 설치돼 있나
```

코루틴이 취소되면 읽기도 멈춘다. 어느 쪽으로 읽었는지는 기기 로그(`AndroidTts`, `IosTts:`)에 한 줄씩 남는다. Android 쪽 끝 알림과 못 트는 파일의 대체는 Robolectric 호스트 테스트로, iOS 쪽 재생, 멈춤, 대체는 시뮬레이터 테스트로 확인한다. 음성 고르는 규칙은 공통 테스트(`BestVoiceTest`)가 가짜 음성 목록으로 본다.

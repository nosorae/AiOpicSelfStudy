# recorder

답변 하나를 몇 초 단위 조각 파일로 녹음하는 라이브러리다. 답하는 도중 앱이 꺼져도 쓰던 조각 하나만 잃는다([ADR 0013](../../docs/adr/0013-녹음-조각-저장과-이어-녹음.md)). 프로젝트의 다른 모듈을 의존하지 않는다.

| 플랫폼 | 구현 | 방식 |
|---|---|---|
| Android | `AndroidRecorder(context)` | `MediaRecorder` 하나를 멈추지 않고 `setNextOutputFile` 로 다음 조각에 넘긴다. 마이크 포그라운드 서비스(`RecordingService`)로 화면이 꺼져도 녹음한다 |
| iOS | `IosRecorder(chunkLength = 5.seconds)` | `AVAudioEngine` 입력을 m4a 조각에 나눠 쓴다 |

```kotlin
recorder.start(dir)                 // 이미 조각이 있으면 그 뒤 번호부터 이어 쓴다. 못 하면 RecordingFailure
val (chunks, failure) = recorder.stop()  // 녹음한 순서대로. 도중에 조각을 못 썼으면 failure 가 있다
val resumedAt = recorder.merge(chunks, "$dir/answer.m4a")
```

- 조각 이름은 녹음 순서 번호라서 앱이 죽었다 켜져도 `chunks(dir)` 로 목록을 되찾는다
- 마이크를 못 열면 `MicUnavailable`, 폴더나 조각 파일을 못 만들거나 못 쓰면 `WriteFailed` 다. 녹음 도중 실패는 녹음을 끊지 않고 `stop` 이 알린다
- 전화 같은 끼어듦이 끝나면 새 조각부터 이어 쓴다. 다시 시작한 조각에는 표시를 붙인다
- `merge` 는 이어 녹음을 시작한 위치를 합친 파일 기준 시각으로 돌려준다. 분석 요청에 넣어 그 위치를 말 멈춤으로 세지 않게 한다
- `recorder.levels` 는 마이크에 들어오는 소리 크기를 0~1 로 80ms 마다 낸다. 녹음 화면의 목소리 띠(`MontageVoiceRibbon`)에 넣는 값이다. 진폭을 -50dB~0dB 구간으로 바꿔서 보통 말소리가 0.5 안팎이 된다. 녹음 중이 아니면 0 이다. Android 는 `MediaRecorder.getMaxAmplitude`, iOS 는 녹음 탭 버퍼의 최대 표본에서 읽는다
- Android 매니페스트에 `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE` 권한과 서비스가 들어 있다. 권한 요청은 앱이 한다

## 테스트

`FakeRecorder` 와 그 테스트가 `commonTest` 에 있다. Android 구현은 마이크와 오디오 포커스가 기기에만 있어서 `androidDeviceTest` 로, iOS 구현은 시뮬레이터 테스트로 확인한다.

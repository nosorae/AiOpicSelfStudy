# s2 기기 음성으로 질문 읽기 품질 (F2.3)

질문 문장 3개를 iOS, Android 기기 음성으로 읽혀 `samples/` 에 m4a(모노 AAC 48kbps)로 두었다. 비교 기준으로 질문 1번만 Gemini 음성으로도 만들었다. 사람이 듣고 아래 세 질문에 답해 `result.json` 을 채우면 이 노드가 끝난다. 판정 결과는 설계의 "기기 음성으로 읽고, 연습이 안 될 만큼 다르면 Gemini 음성으로 미리 만들어 둔다"(`docs/specs/design.md`)를 그대로 둘지 정하는 근거가 된다.

## 질문 문장

`questions.txt` 에 있다. 시험 문항 형식을 흉내 내어 새로 쓴 문장이다.

1. 자기소개 요청
2. 3문항 세트의 첫 문항(서베이에서 고른 공원)
3. 롤플레이(가게에 전화해 운동화 묻기)

## 샘플

| 파일 | 엔진 | 음성 이름 | 길이(초) q1 / q2 / q3 | 말 속도 설정 |
|---|---|---|---|---|
| `ios-sim-default-q{1,2,3}.m4a` | AVSpeechSynthesizer, iPhone 17 시뮬레이터 iOS 26.4 | Samantha (`com.apple.voice.super-compact.en-US.Samantha`) | 6.9 / 9.4 / 9.7 | `AVSpeechUtteranceDefaultSpeechRate` (0.5) |
| `macos-default-q{1,2,3}.m4a` | AVSpeechSynthesizer, macOS 26.4.1 | Samantha (`com.apple.voice.compact.en-US.Samantha`) | 6.9 / 9.4 / 9.7 | `AVSpeechUtteranceDefaultSpeechRate` (0.5) |
| `android-default-q{1,2,3}.m4a` | TextToSpeech `com.google.android.tts`, Pixel 9a 에뮬레이터 API 36 | `en-US-language` (`setLanguage(Locale.US)` 뒤 기본 음성) | 7.0 / 9.7 / 10.3 | 기본값(속도 1.0, 높이 1.0) |
| `gemini-kore-q1.m4a` | Gemini `gemini-2.5-flash-preview-tts` | Kore | 8.0 | 지정 안 함 |

Gemini 호출 한 번은 입력 30토큰, 출력 오디오 199토큰이었다.

## 만든 방법

iOS 는 `ios/main.swift` 하나를 두 번 컴파일했다. iOS 시뮬레이터 SDK 로 컴파일한 실행 파일을 `xcrun simctl spawn <iPhone 17> ...` 으로 시뮬레이터 안에서 돌린 것이 `ios-sim-default` 이고, 같은 소스를 macOS 로 컴파일해 돌린 것이 `macos-default` 다. 둘 다 `write(_:toBufferCallback:)` 로 받은 PCM 버퍼를 파일로 쓴다.

시뮬레이터에는 en-US 음성이 super-compact Samantha 와 품질 1(기본) 음성들밖에 없었고, 이 맥에도 Enhanced, Premium en-US 음성이 설치돼 있지 않았다. 그래서 고품질 음성 샘플은 만들지 못했다. 실기기 iPhone 의 기본 음성은 시뮬레이터보다 한 단계 나은 compact 이거나 사용자가 내려받은 Enhanced 일 수 있다. 실기기 소리에 더 가까운 쪽은 `macos-default` 로 보고, 시뮬레이터 샘플은 최악의 경우로 듣는다. macOS 와 iOS 는 같은 엔진이지만 설치된 음성과 버전이 달라 실기기 소리와 다를 수 있다.

고품질 음성까지 들어 보려면 시뮬레이터나 맥의 설정 > 손쉬운 사용 > 읽기 및 말하기 > 음성에서 영어(미국) Enhanced 또는 Premium 음성을 내려받고 아래 명령을 다시 돌린다. 설치돼 있으면 `ios-hq-q*.caf` 도 함께 나온다.

```sh
swiftc -O spikes/tts/ios/main.swift -o "$TMPDIR/tts-mac"
"$TMPDIR/tts-mac" spikes/tts/questions.txt <출력 폴더>

xcrun --sdk iphonesimulator swiftc -O -target arm64-apple-ios26.0-simulator spikes/tts/ios/main.swift -o "$TMPDIR/tts-sim"
codesign -s - -f "$TMPDIR/tts-sim"
xcrun simctl spawn <시뮬레이터 id> "$TMPDIR/tts-sim" "$PWD/spikes/tts/questions.txt" <출력 폴더>
```

Android 는 `android/` 의 액티비티 하나짜리 APK 로 `synthesizeToFile` 을 불렀다. Gradle 없이 build-tools 로 직접 묶는다. 에뮬레이터를 켠 뒤 `ANDROID_HOME=<sdk> OUT_DIR=<출력 폴더> sh spikes/tts/android/build_and_run.sh` 로 돌린다.

Gemini 샘플은 `GEMINI_API_KEY=... sh spikes/tts/gemini.sh <출력 m4a>` 로 만든다.

샘플 원본(caf, wav)은 ffmpeg 로 모노 AAC 48kbps m4a 로 바꿨다.

## 사람이 판정할 질문

샘플마다 답을 `result.json` 의 같은 이름 항목에 `true` 또는 `false` 로 적는다. 이유는 `note` 에 한 줄로 쓴다.

1. `easy_to_understand`: 처음 한 번 듣고 질문 내용을 알아들을 수 있는가
2. `natural_like_exam`: 실제 시험 질문을 듣는 것처럼 자연스러운가 (억양, 끊어 읽기, 속도)
3. `usable_for_practice`: 이 소리로 연습해도 실제 시험에서 당황하지 않겠는가

마지막에 `decision.device_tts_ok` 에 기기 음성을 그대로 쓸지 적는다. `false` 면 Gemini 음성으로 미리 만들어 두는 쪽으로 설계를 바꾼다.

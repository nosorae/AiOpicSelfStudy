# s2 기기 음성으로 질문 읽기 품질 (F2.3)

질문 문장 3개를 iOS, Android 기기 음성으로 읽혀 `samples/` 에 m4a(모노 AAC 48kbps)로 두었다. 비교 기준으로 질문 1번만 Gemini 음성으로도 만들었다. 판정 결과(`result.json`)는 설계의 "기기 음성으로 읽고, 연습이 안 될 만큼 다르면 Gemini 음성으로 미리 만들어 둔다"(`docs/specs/design.md`)를 그대로 둘지 정하는 근거가 된다.

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

## 판정

사용자가 판정을 맡겨서 귀로 듣는 대신 잴 수 있는 값으로 판정했다. 기기 음성을 기본으로 쓰고, 고품질(Enhanced, Premium) 음성이 설치돼 있으면 그것을 먼저 고르고 없으면 기본 음성을 쓴다.

모든 기기 음성 샘플의 단어 오류율(WER)이 0% 였다. 기준인 5% 이하라서 iOS, Android 기본 음성 모두 연습에 쓸 만하다고 판정했다.

| 샘플 | WER q1 / q2 / q3 | 분당 단어 q1 / q2 / q3 | 문장 안 쉼(초) |
|---|---|---|---|
| `ios-sim-default` | 0 / 0 / 0 | 216 / 211 / 247 | 0.23 ~ 0.30 |
| `macos-default` | 0 / 0 / 0 | 216 / 211 / 247 | 0.23 ~ 0.30 |
| `android-default` | 0 / 0 / 0 | 215 / 204 / 234 | 0.19 ~ 0.72 |
| `gemini-kore` | 0 | 188 | 0.22 ~ 0.48 |

재는 방법은 `measure.py` 에 있고 결과 원본은 `measurements.json` 이다.

- WER: 샘플을 `gemini-3.5-flash-lite` 로 받아써서 원문과 단어 단위로 비교했다. 소문자로 바꾸고 문장 부호를 뗀 뒤 센다. 받아쓰기가 끝 문장을 빠뜨린 일이 한 번 있었다(다른 모델로 잰 `ios-sim-default-q3` 가 40%, 같은 파일을 다시 세 번 받아쓰니 0%). 그래서 처음 WER 이 5% 를 넘으면 두 번 더 받아쓰고 중앙값을 쓰게 했다. 최종 측정에서는 다시 받아쓴 샘플이 없었다
- 분당 단어: 원문 단어 수를 파일 길이로 나눴다. 앞뒤 무음이 길이에 들어가 실제 말 속도는 이보다 조금 빠르다
- 쉼: ffmpeg `silencedetect`(-35dB 아래로 0.15초 이상)로 찾은 무음 가운데 파일 처음과 끝을 뺀 것이다. 문장 사이와 쉼표 뒤의 쉼이 함께 잡힌다

같은 문장(질문 1번)에서 기기 음성은 분당 215~216단어로 Gemini 음성(188단어)보다 15% 빠르다. 알아듣는 데는 문제가 없었지만, 연습용으로 느리게 듣고 싶다는 요구가 생기면 말 속도 설정을 낮춰 다시 잰다.

## 결론

질문은 기기 음성으로 읽는다. 고품질 음성이 설치돼 있으면 그것을 고르고, 없으면 기본 음성을 쓴다.

사용자가 나중에 직접 들어 보고 별로면 설계에 있는 대안(Gemini 음성 미리 만들기)으로 바꾼다. 그렇게 바꿀 수 있게 음성 엔진은 포트 뒤에 둔다.

사람이 귀로 듣는 판정은 사용자에게 맡겨 둔 상태다. 사용자가 판정을 위임해서 이번에는 수치 기준으로 판정했다.

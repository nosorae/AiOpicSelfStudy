# AGENTS.md

오픽(OPIc) 영어 말하기 시험을 실제 순서대로 연습하는 Android, iOS 앱이다. Compose Multiplatform 으로 만들고, 사용자가 넣은 Gemini API 키로 답변 녹음을 분석한다. 서버는 없다.

제품 요구사항은 `docs/specs/product.md`, 설계는 `docs/specs/design.md`, 설계 결정은 `docs/adr/` 에 있다. 요구사항 번호(F1.0 등)는 이슈, 테스트, 커밋에서 그대로 가리킨다.

## 1. 모듈 구조

다섯 층으로 나눈다. 모듈 경로의 첫 마디가 층이다.

| 층 | 모듈 | 하는 일 | 의존해도 되는 층 |
|---|---|---|---|
| 앱 | `apps/opic/shared`, `apps/opic/androidApp`, `apps/opic/iosApp` | 조립만. DI 그래프, 진입점, 내비게이션 등록 | apps, ui, packs, engine, libs |
| 화면 | `ui/design`, `ui/navigation`, `ui/feature-*` | 팩이 주는 문구와 등급 체계를 받아 그린다. "오픽"이라는 말을 코드에 쓰지 않는다 | ui, engine, libs |
| 시험 팩 | `packs/opic-en` | 서베이, 문항 순서, 체크리스트, 프롬프트, 내장 문항, 등급 | engine, libs |
| 엔진 | `engine/session`, `engine/answer`, `engine/question` | 시험 진행 계산과 이어 보기, 답변 분석 흐름과 출력 검사, 출제 | engine, libs |
| 라이브러리 | `libs/recorder`, `libs/llm-gemini`, `libs/work-queue`, `libs/keyvault`, `libs/tts`, `libs/timer` | 플랫폼 기능과 외부 API. 폴더째 떼어 쓸 수 있다 | 없음 (프로젝트 모듈 의존 0개) |

`tools/eval` 은 JVM 명령줄 도구이고 engine, packs, libs 만 의존한다.

의존 규칙은 `build-logic` 의 `LayerRules.kt` 에 표 하나로 있고, `./gradlew check` 의 `checkLayers` 가 어긴 의존마다 고칠 방법을 찍고 실패한다. 화면은 시험 팩을 모른다. 팩은 앱이 조립해서 넘긴다.

새 모듈은 `settings.gradle.kts` 에 넣고 층에 맞는 플러그인 id 하나만 건다.

| 플러그인 id | 쓰는 곳 |
|---|---|
| `aiopic.kmp.library` | engine, packs |
| `aiopic.kmp.library.explicit` | libs (explicit API 모드) |
| `aiopic.kmp.compose` | `ui/design`, `ui/navigation`, `apps/opic/shared` |
| `aiopic.ui.feature` | `ui/feature-*` (디자인, 내비게이션, ViewModel, 코루틴 테스트 의존 포함) |
| `aiopic.android.application` | `apps/opic/androidApp` |

Android namespace 와 Compose 리소스 패키지는 모듈 경로로 만든다. `:ui:feature-exam` 은 `io.github.nosorae.aiopic.ui.featureexam` 이다. SDK, JVM 버전은 `gradle/libs.versions.toml` 에만 쓴다.

## 2. 명령

저장소 루트에서 실행한다.

| 목적 | 명령 |
|---|---|
| 전체 확인 (층 검사, 테스트, build-logic 테스트) | `./gradlew check` |
| 모듈 하나 테스트 | `./gradlew :engine:session:allTests` |
| Android 호스트 테스트만 | `./gradlew :engine:session:testAndroidHostTest` |
| iOS 시뮬레이터 테스트만 | `./gradlew :engine:session:iosSimulatorArm64Test` |
| Android 앱 빌드 | `./gradlew :apps:opic:androidApp:assembleDebug` |
| iOS 프레임워크 링크 | `./gradlew :apps:opic:shared:linkDebugFrameworkIosSimulatorArm64` |
| iOS 앱 빌드 | `xcodebuild -project apps/opic/iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath build/xcode build` |

iOS 시뮬레이터는 iPhone 17 하나로 맞춘다. 스크린샷과 시험 결과를 기기마다 다르게 만들지 않기 위해서다.

실기기에서 iOS 앱을 돌리려면 `apps/opic/iosApp/Configuration/Local.xcconfig.example` 을 같은 폴더의 `Local.xcconfig` 로 복사해 자기 Apple 개발 팀 ID 를 `TEAM_ID` 에 넣는다. `Config.xcconfig` 가 이 파일을 불러온다. `Local.xcconfig` 는 커밋하지 않는다.

## 3. 작업 규칙

### 이슈와 브랜치

1. 추적되는 파일을 바꾸는 일은 모두 이슈를 먼저 만든다. 이슈 템플릿 절은 배경, 할 일, 완료 조건, 고칠 범위, 먼저 끝날 것, 노드다
2. 브랜치 이름은 `node/<노드id>` 다. 개발 그래프 밖의 손질도 이슈 번호로 노드 id 를 정한다
3. 노드를 시작할 때와 끝낼 때 `scripts/graph/set_status.sh` 로 노드 상태를 바꾼다. 인자는 스크립트 도움말을 따른다
4. 이슈의 고칠 범위 밖 파일은 꼭 필요할 때만 고치고, 그 이유를 PR 본문에 적는다

### 커밋

- 형식: `[노드id] 무엇을 했다 (#이슈번호)`. 예: `[n-state] 40분 타이머가 앱 종료 중에 멈추게 했다 (#12)`
- 한 커밋에 한 가지 변경. 이미 만든 커밋을 고치지 않고 새 커밋을 쌓는다
- 공개될 오픈소스라 민감 정보를 커밋하지 않는다. API 키, 토큰, 서명 팀 ID, 인증서, 개인 이메일과 기기 이름, 개인 PC 절대경로가 그렇다. 커밋, 이슈, PR, 코드 어디에도 쓰지 않는다
- 기기마다 다른 값은 커밋하지 않는 파일(`local.properties`, `apps/opic/iosApp/Configuration/Local.xcconfig`)에 둔다
- Gemini API 키는 사용자가 앱 설정 화면에서 넣는 값이다. 코드와 테스트 고정값에도 실제 키를 넣지 않는다. 테스트는 `"test-key"` 같은 가짜 문자열을 쓴다

### PR

노드 하나에 PR 하나다. base 는 `main` 이고 본문은 아래 절을 따른다.

```
## 무엇을 왜
## 바뀐 것
## 확인
(실행한 명령과 결과)
## 스크린샷
(UI 변경이 없으면 "UI 변경 없음")
closes #이슈번호
```

UI 가 바뀌는 PR 은 바뀌기 전과 후 스크린샷을 나란히 붙인다. 병합 전에 `git fetch origin main && git rebase origin/main` 으로 최신화하고 `gh pr merge --squash --delete-branch` 로 바로 병합한다. CI 는 기다리지 않는다. CI 가 깨지면 고치는 이슈를 새로 만든다.

### 구현

- 테스트를 먼저 쓴다. 실패하는 것을 보고 나서 구현한다
- 완료는 명령의 종료 코드로 판정한다. 이슈의 완료 조건 명령과 `./gradlew check` 가 0 으로 끝나야 끝난 것이다. "다 됐다"는 말로 판정하지 않는다
- 막히면 에러 메시지가 가리키는 곳을 고친다. `--no-verify`, `--force`, 테스트 건너뛰기, 검사 끄기로 우회하지 않는다
- 같은 노드에서 세 번 실패하면 멈추고 이슈에 막힌 이유를 적는다

### 문서

- 문서와 주석은 한국어로 쓴다. 문장은 `~한다` 로 끝낸다
- 설계 결정을 새로 하거나 바꾸면 `docs/adr/` 에 기록을 더한다(`adr` 스킬)
- 결정 이유는 이 앱의 기술 사정과 제품 판단으로 쓴다. 다른 프로젝트나 외부 권위를 근거로 들지 않는다

## 4. 스킬

`.claude/skills/` 아래에 있다. 작업에 맞는 스킬을 먼저 읽는다.

| 스킬 | 언제 |
|---|---|
| `architecture` | ViewModel, UseCase, 화면, 이동, 상태, DI, 결과 타입을 만들거나 고칠 때 |
| `testing` | 테스트를 쓸 때. Fake, 디스패처 교체, ViewModel 테스트, 스크린샷 |
| `adr` | 설계 결정을 기록할 때 |
| `navigation-3` | Navigation 3 백스택, 결과 전달 |
| `edge-to-edge` | 시스템 바, 키보드 인셋 처리 |
| `android-permissions-security` | 마이크 권한, 포그라운드 서비스 |
| `android-cli` | 에뮬레이터 실행, 스크린샷, UI 확인 |
| `kotlin-tooling-agp9-migration` | AGP 9 와 KMP 플러그인 구성 |
| `kotlin-tooling-native-build-performance` | iOS 링크가 느릴 때 |
| `kotlin-multiplatform-libraries-expert` (폴더 `kmp-libraries-expert`) | KMP 라이브러리 좌표와 iOS 지원 확인 (klibs MCP 와 함께) |

뒤의 일곱 개는 외부에서 그대로 가져온 스킬이다. 출처와 라이선스는 `.claude/skills/THIRD_PARTY.md` 에 있다. 소스셋별 규칙은 `.claude/rules/` 에 있다.

# CLAUDE.md

오픽(OPIc) 영어 말하기 시험을 실제 순서대로 연습하는 Android, iOS 앱이다. Compose Multiplatform 으로 만들고, 사용자가 넣은 Gemini API 키로 답변 녹음을 분석한다. 서버는 없다.

요구사항 `docs/specs/product.md`, 설계 `docs/specs/design.md`, 결정 `docs/adr/`, 작업 순서 `docs/graph/dev-graph.json`. 요구사항 번호(F1.0 등)는 이슈, 테스트, 커밋에서 그대로 가리킨다.

## 명령

| 목적 | 명령 |
|---|---|
| 전체 확인 (층 검사, ktlint, Lint, 테스트) | `./gradlew check` |
| 포맷 고침 | `./gradlew spotlessApply` |
| 모듈 하나 테스트 | `./gradlew :engine:session:allTests` |
| Android 앱 | `./gradlew :apps:opic:androidApp:assembleDebug` |
| iOS 앱 | `xcodebuild -project apps/opic/iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath build/xcode build` |

## 꼭 지킬 것

- 추적되는 파일을 바꾸는 일은 이슈를 먼저 만들고 `node/<id>` 브랜치에서 PR 로 병합한다. main 에 직접 push 하지 않는다. 절차는 `node-workflow` 스킬
- 끝났다는 판정은 종료 코드로만 한다. `--no-verify`, `--force`, 테스트 건너뛰기로 우회하지 않는다
- 공개 저장소다. API 키, 토큰, 서명 팀 ID, 개인 경로를 어디에도 쓰지 않는다
- 코드는 덜 쓰는 쪽이 기본이다(ponytail 플러그인, ADR 0024). 필요한가, 이미 있나, 표준과 플랫폼으로 되나를 먼저 묻는다. 테스트용 경계와 분석 포트(ADR 0019)는 인터페이스를 둔다
- 테스트를 먼저 쓴다
- 문서와 주석은 한국어 `~한다` 체. 결정 이유는 이 앱의 사정으로 쓰고 외부 권위를 근거로 들지 않는다. 결정을 새로 하면 `adr` 스킬로 기록한다

## 어디를 보나

- 빌드 파일과 모듈 구조: `.claude/rules/build.md` (빌드 파일을 열면 같이 읽힌다)
- 소스셋 규칙: `.claude/rules/commonMain.md`, `androidMain.md`, `iosMain.md`
- ViewModel, UseCase, 화면, DI: `architecture` 스킬. 테스트: `testing` 스킬
- 화면, 테마, 컴포넌트: 언제나 `ui-quality` 스킬 먼저
- 외부에서 가져온 스킬의 출처와 라이선스: `.claude/skills/THIRD_PARTY.md`
- KMP 라이브러리 좌표와 iOS 지원: `.mcp.json` 의 klibs MCP

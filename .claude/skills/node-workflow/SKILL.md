---
name: node-workflow
description: 개발 그래프 노드 하나를 이슈에서 병합까지 진행하는 절차. 이슈, 브랜치, 커밋, PR, 안쪽 루프(done 명령, 리뷰, blocked), 민감 정보, 실기기 서명. 노드를 시작하거나 PR 을 올리거나 병합할 때 먼저 읽는다.
---

# 노드 진행

## 이슈와 브랜치

1. 추적되는 파일을 바꾸는 일은 모두 이슈를 먼저 만든다. 이슈 템플릿 절은 배경, 할 일, 완료 조건, 고칠 범위, 먼저 끝날 것, 노드다
2. 브랜치 이름은 `node/<노드id>` 다. 개발 그래프 밖의 손질도 이슈 번호로 노드 id 를 정한다
3. 이슈의 고칠 범위 밖 파일은 꼭 필요할 때만 고치고, 그 이유를 PR 본문에 적는다

## 안쪽 루프

노드의 `done` 은 실행 명령이고 `done_desc` 는 그 설명이다. 반복 규칙은 `docs/graph/dev-graph.json` 최상위 `loop` 하나에 있고 노드의 `loop` 가 횟수 상한을 덮어쓴다(ADR 0020).

1. `scripts/graph/set_status.sh <노드id> doing`
2. 구현: 테스트를 먼저 쓴다. 실패하는 것을 보고 나서 구현한다
3. done: `scripts/graph/run_done.sh <노드id>` 와 `./gradlew check` 가 0 으로 끝나야 통과다. 실패하면 2로 돌아간다. 실패 횟수는 노드 `attempts.done` 에 남고, 상한(기본 3번)을 넘으면 스크립트가 노드를 blocked 로 바꾼다
4. 리뷰: 다른 컨텍스트의 리뷰 에이전트가 diff 만 보고 검토한다. 지울 수 있는 코드부터 찾는다. 반려되면 `scripts/graph/run_done.sh <노드id> review` 로 기록하고 2로 돌아간다. 상한(기본 2번)을 넘으면 blocked
5. 병합: `set_status.sh <노드id> done` 을 돌려 바뀐 `docs/graph/` 를 같은 PR 에 커밋한다. 상태만 바꾸는 커밋을 main 에 따로 올리지 않는다
6. blocked 가 되면 멈추고 이슈에 막힌 이유를 적고 사람을 부른다

- done 이 `"human"` 인 노드는 사람이 `done_desc` 를 보고 확인한다. 에이전트가 done 으로 바꾸지 않는다
- "다 됐다"는 말로 판정하지 않는다. 종료 코드로만 판정한다
- 막히면 에러 메시지가 가리키는 곳을 고친다. `--no-verify`, `--force`, 테스트 건너뛰기, 검사 끄기로 우회하지 않는다
- 그래프를 고치면 `python3 scripts/graph/validate.py` 와 `python3 -m unittest discover -s scripts/graph` 를 돌린다

## 커밋

- 형식: `[노드id] 무엇을 했다 (#이슈번호)`. 예: `[n-state] 40분 타이머가 앱 종료 중에 멈추게 했다 (#12)`
- 한 커밋에 한 가지 변경. 이미 만든 커밋을 고치지 않고 새 커밋을 쌓는다

## PR

노드 하나에 PR 하나다. base 는 `main` 이고 본문은 `.github/pull_request_template.md` 를 따른다. UI 가 바뀌는 PR 은 바뀌기 전과 후 스크린샷을 나란히 붙인다(`scripts/screenshots/compare.py <PR 번호>`).

병합 전에 `git fetch origin main && git rebase origin/main` 으로 최신화하고 `gh pr merge --squash --delete-branch` 로 바로 병합한다. CI 는 기다리지 않는다. CI 가 깨지면 고치는 이슈를 새로 만든다. pre-push 훅(`scripts/setup-hooks.sh` 로 설치)은 main 으로 가는 push 를 거부하고, 다른 브랜치 push 에서는 `./gradlew check` 를 돈다.

## 민감 정보

공개 저장소다. API 키, 토큰, 서명 팀 ID, 인증서, 개인 이메일과 기기 이름, 개인 PC 절대경로를 커밋, 이슈, PR, 코드 어디에도 쓰지 않는다(ADR 0022).

- 기기마다 다른 값은 커밋하지 않는 파일(`local.properties`, `apps/opic/iosApp/Configuration/Local.xcconfig`)에 둔다
- 테스트는 `"test-key"` 같은 가짜 키를 쓴다
- 실제 Gemini API 를 부르는 노드는 키를 환경 변수 `GEMINI_API_KEY` 에서만 읽는다. 없으면 `scripts/graph/set_status.sh <노드id> blocked "GEMINI_API_KEY 없음"`

## iOS 실기기

`apps/opic/iosApp/Configuration/Local.xcconfig.example` 을 같은 폴더의 `Local.xcconfig` 로 복사해 자기 Apple 개발 팀 ID 를 `TEAM_ID` 에 넣는다. `Config.xcconfig` 가 이 파일을 불러온다. 시뮬레이터는 iPhone 17 하나로 맞춘다. 스크린샷과 시험 결과를 기기마다 다르게 만들지 않기 위해서다.

## 그 밖의 명령

| 목적 | 명령 |
|---|---|
| Android Lint 만 (보고서: 모듈의 `build/reports/lint-results*.html`) | `./gradlew lint` |
| Android 호스트 테스트만 | `./gradlew :engine:session:testAndroidHostTest` |
| iOS 시뮬레이터 테스트만 | `./gradlew :engine:session:iosSimulatorArm64Test` |
| iOS 프레임워크 링크 | `./gradlew :apps:opic:shared:linkDebugFrameworkIosSimulatorArm64` |
| 스크린샷 다시 찍기 | `./gradlew recordRoborazziAndroidHostTest` |

긴 Gradle 명령은 백그라운드로 돌리고 종료 코드를 확인한다. Kotlin 코드 탐색은 kotlin-lsp 플러그인을 쓰고, 실행 파일이 없으면 grep 으로 대신한다.

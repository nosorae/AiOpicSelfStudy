@AGENTS.md

## Claude Code 메모

- 소스셋별 규칙은 `.claude/rules/commonMain.md`, `androidMain.md`, `iosMain.md` 에 있다. 그 소스셋 파일을 고치기 전에 읽는다
- 프로젝트 스킬(`architecture`, `testing`, `adr`)은 Skill 도구로 부른다
- KMP 라이브러리를 고를 때는 `.mcp.json` 의 klibs MCP 로 최신 좌표와 iOS 지원을 확인한다
- Kotlin 코드 탐색은 kotlin-lsp 플러그인(`.claude/settings.json`)을 쓴다. 실행 파일이 없으면 grep 으로 대신한다
- 긴 Gradle 명령은 백그라운드로 돌리고 종료 코드를 확인한다

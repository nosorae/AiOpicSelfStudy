# 0023. 정적 검사는 ktlint, Compose 규칙, Android Lint 를 check 하나에 넣는다

- 상태: 채택
- 날짜: 2026-10-09

## 배경

여러 에이전트가 동시에 코드를 쓴다. 포맷과 Compose 작성 규칙이 사람마다 다르면 리뷰가 형식 지적으로 채워지고, Android 리소스나 API 수준 문제는 실행해 보기 전까지 드러나지 않는다. 로컬에서 통과한 것이 CI 에서 깨지면 고치는 이슈가 계속 생긴다.

## 후보

1. 정적 검사 없이 리뷰에서 본다
2. 검사를 따로따로 돌리는 명령으로 두고 필요할 때 실행한다
3. ktlint(Spotless) 와 Compose 규칙 묶음, 모든 모듈의 Android Lint 를 `./gradlew check` 에 넣고 pre-push 훅과 CI 가 같은 check 를 돈다

## 결정

3을 고른다.

- Spotless 를 루트에 한 번 걸어 모든 `.kt`, `.kts` 에 ktlint 를 돌린다. `.kt` 에는 Compose 규칙 묶음을 더한다
- KMP 라이브러리 모듈과 Android 앱 모듈 모두 Android Lint 를 건다. 오류는 빌드를 멈추고 경고는 보고서에만 남긴다
- pre-push 훅과 CI 는 같은 `./gradlew check` 를 돈다. 포맷은 `./gradlew spotlessApply` 로 고친다

## 이유

명령 하나가 통과하면 끝이라는 기준이 있어야 에이전트가 완료를 종료 코드로 판정할 수 있다. 검사를 따로 두면 빠뜨린 검사가 CI 에서야 드러난다. 루트에 한 번 걸고 convention 플러그인에 넣으면 새 모듈이 설정을 잊을 일이 없다. Lint 경고까지 실패로 두면 아직 코드가 적은 지금 의미 없는 경고에 막혀서 오류만 멈춘다.

## 결과

check 가 길어진다. 모듈 하나만 볼 때는 모듈 테스트 명령을 쓰고, 끝내기 전에 check 를 돈다. Lint 경고가 쌓이면 보고서를 보고 규칙을 오류로 올리는 이슈를 만든다.

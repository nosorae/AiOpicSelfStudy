# 0008. Intent 이름은 동작 이름으로 짓는다

- 상태: 채택
- 날짜: 2026-10-09

## 배경

`sealed interface XxxIntent` 의 각 경우에 이름을 붙이는 규칙이 필요하다.

## 후보

1. `StartClicked` 처럼 UI 이벤트 이름
2. `StartExam` 처럼 사용자가 하려는 동작 이름

## 결정

Intent 는 동작 이름(`StartExam`, `ReplayQuestion`, `SubmitAnswer`)으로 짓는다. `Clicked` 는 컴포저블 람다 이름(`onStartClicked`)에만 쓴다.

## 이유

같은 동작이 버튼, 제스처, 키보드 어디서 와도 이름이 맞다. ViewModel 은 무엇을 할지만 알고 어떻게 눌렸는지는 모른다. 람다 이름은 UI 쪽이라 `Clicked` 가 맞다.

## 결과

람다와 Intent 이름이 달라 화면 최상위에서 한 번 이어 준다(`onStartClicked = { onIntent(StartExam) }`).

## 근거

- [ExamIntent](../../ui/feature-exam/src/commonMain/kotlin/io/github/nosorae/aiopic/ui/featureexam/ExamUiState.kt)

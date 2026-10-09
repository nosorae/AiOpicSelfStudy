# 0005. 화면 상태는 UiState 하나, 기본은 MutableStateFlow

- 상태: 채택
- 날짜: 2026-10-09

## 배경

ViewModel 이 화면에 내보내는 상태를 어떻게 만들지 정해야 한다. 이 앱 화면 대부분(시험 진행, 키 입력, 동의)은 사용자 Intent 로 상태가 바뀌고, 기록 화면만 저장소 Flow 를 그대로 보여 준다.

## 후보

1. `MutableStateFlow` + `update` 를 기본으로
2. `stateIn(WhileSubscribed(5000))` 을 기본으로
3. 데이터 층 Flow 가 있으면 무조건 `stateIn`

## 결정

화면마다 `XxxUiState` data class 하나를 `StateFlow` 로 내보낸다. 기본은 `MutableStateFlow` + `update` 다.

- 저장소 Flow 를 그대로 보여 주는 기록 화면만 `stateIn` 을 쓴다
- 리포트처럼 뒤에서 데이터가 바뀌는 다른 화면은 저장소 Flow 를 구독해 `update` 로 옮긴다
- ViewModel 에 `mutableStateOf` 를 두지 않는다. 파생 값은 UiState 의 계산 프로퍼티로
- 화면은 `collectAsStateWithLifecycle()` 로 모은다

## 이유

상태가 언제 바뀌는지가 `onIntent` 핸들러에 모두 보인다. 일회성 필드(0006)도 같은 `update` 로 다룬다. ViewModel 테스트가 `uiState.value` 하나만 보면 된다.

## 결과

구독자가 없어도 저장소 Flow 구독이 ViewModel 수명 동안 살아 있는 화면이 생긴다. 리포트 화면은 짧게 머무는 화면이라 비용이 작다. 이런 화면이 늘면 `stateIn` 쪽으로 다시 본다.

## 근거

- [UiState 예](../../ui/feature-exam/src/commonMain/kotlin/io/github/nosorae/aiopic/ui/featureexam/ExamUiState.kt)
- [MutableStateFlow 와 update](../../ui/feature-exam/src/commonMain/kotlin/io/github/nosorae/aiopic/ui/featureexam/ExamViewModel.kt)
- [stateIn 을 쓰는 기록 화면](../../ui/feature-history/src/commonMain/kotlin/io/github/nosorae/aiopic/ui/featurehistory/HistoryViewModel.kt)

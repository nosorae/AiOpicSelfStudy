# 0006. 일회성 이벤트는 상태 필드와 처리 완료 Intent 로

- 상태: 채택
- 날짜: 2026-10-09

## 배경

화면 이동, 안내 메시지처럼 한 번만 일어나야 하는 일을 ViewModel 이 화면에 알려야 한다.

## 후보

1. Channel 이나 SharedFlow 로 이벤트를 보낸다
2. UiState 필드(`navigation`, `message`)에 두고 화면이 처리한 뒤 처리 완료 Intent(`NavigationHandled`, `MessageShown`)로 지운다

## 결정

UiState 필드와 처리 완료 Intent 를 쓴다. Channel, SharedFlow 이벤트는 쓰지 않는다.

## 이유

화면이 처리했다고 알려야 상태가 지워지므로 처리를 빠뜨릴 수 없다. 화면을 돌려도 값이 남는다. ViewModel 테스트는 상태만 보면 되고 이벤트를 따로 모을 도구가 필요 없다.

## 결과

처리 완료 Intent 가 이벤트마다 하나씩 늘어난다. ViewModel 상태는 프로세스와 함께 사라지므로 앱 재시작 뒤 복원은 이 필드가 아니라 Room 의 시험 상태(0012)가 맡는다.

## 근거

- [navigation 필드와 NavigationHandled](../../ui/feature-exam/src/commonMain/kotlin/io/github/nosorae/aiopic/ui/featureexam/ExamUiState.kt)
- [처리 완료를 보내는 화면](../../ui/feature-exam/src/commonMain/kotlin/io/github/nosorae/aiopic/ui/featureexam/ExamScreen.kt)

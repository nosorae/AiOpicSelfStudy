# 0007. 화면 인자는 assisted 주입으로 받는다

- 상태: 채택
- 날짜: 2026-10-09

## 배경

답변 리포트처럼 인자(답변 ID)가 있는 화면은 ViewModel 이 그 인자를 알아야 한다.

## 후보

1. 화면이 열릴 때 `Entered(id)` Intent 를 보낸다
2. Metro assisted 주입으로 ViewModel 생성자에 받는다

## 결정

assisted 주입으로 생성자에 받는다. 인자는 직렬화 가능한 `NavKey` 에 담는다. 진입 Intent 는 만들지 않는다.

## 이유

`NavKey` 가 백스택과 함께 복원되므로 프로세스가 죽었다 살아나도 인자가 남는다. 진입 Intent 방식은 화면을 다시 그릴 때 진입이 두 번 들어오는 경우를 따로 막아야 한다. 생성자에서 인자를 받으면 ViewModel 의 필드가 처음부터 정해져 null 처리도 없다.

## 결과

화면마다 assisted 팩토리가 하나씩 생긴다.

## 근거

- [assisted 주입 ViewModel](../../ui/feature-exam/src/commonMain/kotlin/io/github/nosorae/aiopic/ui/featureexam/ExamViewModel.kt)
- [화면 키](../../apps/opic/shared/src/commonMain/kotlin/io/github/nosorae/aiopic/apps/opic/shared/AppRoute.kt)

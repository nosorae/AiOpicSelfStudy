# 0003. 화면 모듈을 api, impl 로 나누지 않는다

- 상태: 일부 대체됨([0034](0034-화면-경로-키와-등록은-앱-층-AppRoute-한-곳에.md)). 키와 화면 등록을 `ui/navigation` 과 화면 모듈에 두는 부분
- 날짜: 2026-10-09

## 배경

화면 모듈끼리 서로 이동하려면 상대 화면의 내비게이션 키를 알아야 한다. 키를 어디에 둘지에 따라 모듈 수와 빌드 그래프가 달라진다.

## 후보

1. 화면마다 `api`(키)와 `impl`(구현) 모듈 둘
2. 화면마다 모듈 하나, 내비게이션 키는 `ui/navigation` 한 곳에

## 결정

화면마다 모듈 하나만 둔다. 모든 내비게이션 키는 `ui/navigation` 에 모은다. 화면 등록은 화면 모듈의 함수로 하고, 백스택 조립은 앱 층에서 한다.

## 이유

화면이 여섯 개 남짓인 앱에서 api, impl 로 나누면 모듈만 두 배가 되고 얻는 빌드 격리는 작다. 키가 한 곳에 있으면 이동 지도를 한 파일에서 본다.

## 결과

키 모듈이 바뀌면 모든 화면 모듈이 다시 컴파일된다. 키는 작은 직렬화 클래스라 자주 바뀌지 않는다. 화면 수가 크게 늘어 빌드 시간이 문제가 되면 다시 본다.

## 근거

- [화면 경로 표](../../apps/opic/shared/src/commonMain/kotlin/io/github/nosorae/aiopic/apps/opic/shared/AppRoute.kt)
- [화면 등록](../../apps/opic/shared/src/commonMain/kotlin/io/github/nosorae/aiopic/apps/opic/shared/RouteContent.kt)

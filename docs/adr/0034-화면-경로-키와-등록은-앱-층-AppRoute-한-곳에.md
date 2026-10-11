# 0034. 화면 경로 키와 화면 등록은 앱 층 AppRoute 한 곳에 둔다

- 상태: 채택
- 날짜: 2026-10-12
- 대체: 0003 의 '키는 `ui/navigation`, 화면 등록은 화면 모듈 함수' 부분, 0007 의 '인자는 직렬화 가능한 `NavKey` 에 담는다' 부분. assisted 주입으로 인자를 받는 결정(0007)과 화면 모듈을 api, impl 로 나누지 않는 결정(0003)은 그대로다

## 배경

architecture 스킬 5절은 키를 `ui/navigation` 의 직렬화 가능한 `NavKey` 로, 화면 등록을 화면 모듈의 Navigation 3 등록 함수로 두기로 했다. 앱 틀을 만들고 보니 두 플랫폼이 같은 방식으로 이동하지 않는다. Android 는 Navigation 3 의 `NavDisplay` 이고, iOS 는 SwiftUI `TabView` 안에 탭마다 `NavigationStack` 을 둔다(ADR 0018). 하단 탭 표시, 뒤로가기 가로채기, iOS 뒤로 버튼을 정하는 표도 키마다 하나씩 필요하다.

## 후보

1. 키만 `ui/navigation` 으로 옮기고, 탭과 뒤로가기 표는 앱 층의 확장 함수로 둔다: 키 하나를 두 모듈에서 따로 읽어 새 화면을 더할 때 두 곳을 고친다
2. 키, 표, 화면 등록을 앱 층(`apps/opic/shared`)의 `AppRoute` 와 `RouteContent` 에 둔다
3. 스킬대로 화면 모듈마다 Navigation 3 등록 함수를 둔다: Android 에서만 쓸 수 있어 iOS 는 화면 등록을 따로 한 벌 더 만든다

## 결정

후보 2를 고른다.

- 키는 `apps/opic/shared` commonMain 의 `@Serializable sealed class AppRoute` 하나다. `NavKey` 를 구현하지 않는다. 화면 인자(회차 id, 답변 id, 제공사)는 키의 속성이다
- 탭 표시(`showsTabBar`), 뒤로가기 가로채기(`blocksBack`), iOS 뒤로 버튼(`showsBackButtonOnIos`), 제목, 탭 순서(`tabs`)를 `AppRoute` 에 둔다
- 화면 등록은 commonMain 의 `RouteContent` 의 `when` 하나다. 두 플랫폼 앱 틀이 이 함수를 같이 부른다
- 백스택은 앱 틀이 든다. Android `AppShell` 은 하위 화면에서 탭을 숨기므로 백스택 하나에 모의고사 탭을 맨 아래 둔다. iOS `ContentView.swift` 는 탭마다 경로를 따로 둔다
- 화면 모듈은 `AppRoute` 를 모른다. 이동은 `XxxRoute` 의 람다로 받고, 앱 층이 람다에서 다음 `AppRoute` 를 쌓는다
- `ui/navigation` 모듈은 비워 둔다. 화면 모듈끼리 키를 나눠 쓸 일이 생기면 그때 채운다

## 이유

iOS 는 Navigation 3 를 쓰지 않으므로 화면 모듈의 Navigation 3 등록 함수는 Android 만 쓸 수 있다. 등록을 commonMain 의 함수 하나로 두면 두 플랫폼이 같은 화면, 같은 인자로 그린다. 새 화면을 더할 때 고치는 곳은 `AppRoute` 의 case 와 `RouteContent` 의 가지 둘이고, 빠뜨리면 `when` 이 컴파일되지 않는다.

`AppRoute` 를 interface 가 아닌 class 로 둔 것은 Swift 의 `NavigationStack` 경로로 그대로 쓰기 위해서다. Kotlin 클래스는 Swift 에서 Hashable 이다. commonMain 에서 `NavKey` 를 구현하려면 Navigation 3 런타임을 iOS 까지 공통 의존성으로 끌어와야 하는데, iOS 에서는 쓰지 않는다.

## 결과

- 화면 모듈을 다른 앱에서 다시 쓰려면 그 앱이 자기 경로 표와 `RouteContent` 를 새로 만든다
- Android 는 백스택을 직렬화해 저장하므로 프로세스가 죽어도 키와 인자가 남는다. iOS 는 경로를 저장하지 않아 다시 켜면 모의고사 탭부터 보인다. 시험은 Room 에 저장된 회차로 이어 본다(ADR 0012)
- 화면 모듈이 다른 화면 모듈로 직접 이동해야 하면 후보 1을 다시 본다

## 근거

- [화면 경로 표](../../apps/opic/shared/src/commonMain/kotlin/io/github/nosorae/aiopic/apps/opic/shared/AppRoute.kt)
- [화면 등록](../../apps/opic/shared/src/commonMain/kotlin/io/github/nosorae/aiopic/apps/opic/shared/RouteContent.kt)
- [Android 앱 틀](../../apps/opic/shared/src/androidMain/kotlin/io/github/nosorae/aiopic/apps/opic/shared/AppShell.kt)
- [iOS 앱 틀](../../apps/opic/iosApp/iosApp/ContentView.swift)

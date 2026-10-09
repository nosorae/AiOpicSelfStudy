# 0004. 의존성 주입은 Metro 로 한다

- 상태: 채택
- 날짜: 2026-10-09

## 배경

Android, iOS 공통 코드에서 ViewModel, UseCase, 저장소를 연결할 DI 가 필요하다. Hilt 는 Kotlin Multiplatform 을 지원하지 않는다.

## 후보

1. Metro: 컴파일 타임에 그래프를 만든다
2. Koin: 실행 시점에 찾는다
3. 손으로 짠 생성자 연결

## 결정

Metro 를 쓴다.

- 그래프는 플랫폼마다 하나, 앱 층(`apps/opic/shared`)에서 조립한다. iOS 그래프도 Kotlin iosMain 에서 만든다
- libs 에는 DI 주석을 쓰지 않는다. 앱 층에서 `@Provides` 로 연결한다

## 이유

바인딩이 빠지면 빌드가 실패한다. 실행 시점 DI 는 빠진 바인딩을 화면을 열어 봐야 알고, iOS 에서는 그 확인이 더 늦다. 화면 인자를 생성자로 받는 assisted 주입(0007)도 지원한다. libs 에 주석을 두지 않으면 libs 를 떼어 쓸 때 DI 라이브러리가 따라가지 않는다.

## 결과

컴파일러 플러그인이라 Kotlin 버전을 올릴 때 Metro 지원 버전을 함께 확인해야 한다.

## 근거

- [앱 DI 그래프](../../apps/opic/shared/src/commonMain/kotlin/io/github/nosorae/aiopic/apps/opic/shared/AppGraph.kt)

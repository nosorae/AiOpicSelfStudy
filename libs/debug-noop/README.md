# debug-noop

[`libs/debug`](../debug/README.md) 와 패키지, 공개 API 가 같은 빈 모듈이다. 릴리스 빌드에 넣는다. 아무것도 기록하지 않고 스위치는 늘 기본값을 돌려준다. 두 모듈의 API 가 같은지는 `./gradlew check` 의 `checkNoopAbi` 가 본다. `open()` 은 아무 일도 안 하고, iOS `viewController()` 는 빈 화면을 돌려준다.

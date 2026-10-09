# timer

기기가 잠든 동안에도 흐르고, 사용자가 시각을 바꿔도 거꾸로 가지 않는 단조 시계다. 40분 시험 타이머와 녹음 시간 규칙이 화면을 잠근 채 녹음하는 동안에도 맞아야 해서 둔다. 프로젝트의 다른 모듈을 의존하지 않는다.

| 플랫폼 | 값 |
|---|---|
| Android | `SystemClock.elapsedRealtimeNanos()` |
| iOS | `clock_gettime_nsec_np(CLOCK_MONOTONIC)` |

```kotlin
val timer = deviceMonotonicTimer()
val start = timer.elapsed()
// ...
val spent = timer.elapsed() - start
```

`elapsed()` 는 기기 부팅부터 흐른 시간이라 두 값의 차이로만 쓴다. 앱을 다시 켜면 시작점이 의미 없어지므로, 쓰는 쪽이 흐른 시간을 끊어서 저장한다.

테스트에서는 `MonotonicTimer { elapsed }` 처럼 람다로 바꿔 끼운다. 기기 시계 쪽은 Android 호스트 테스트(Robolectric 으로 시계를 앞으로 돌린다)와 iOS 시뮬레이터 테스트로 확인한다.

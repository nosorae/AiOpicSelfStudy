# work-queue

앱이 꺼지거나 오프라인이어도 남는 일을 비우는 플랫폼 대기열이다. 일 자체는 앱이 DB 에 두고, 이 라이브러리는 언제 비울지만 정한다. 연결돼 있을 때만, 한 번에 하나씩 `Drain` 을 부른다. 프로젝트의 다른 모듈을 의존하지 않는다.

| 플랫폼 | 구현 | 방식 |
|---|---|---|
| Android | `WorkManagerWorkQueue(context, drain)` | 고유 작업 사슬 하나에 비우기 작업을 이어 붙인다. 네트워크가 연결돼야 돌고 앱이 꺼져도 WorkManager 가 깨운다 |
| iOS | `IosWorkQueue(drain, scope, backgroundTask = "...")` | 앱이 앞에 있고 연결돼 있는 동안 비운다. 비우는 동안은 앱이 뒤로 가도 끝낼 시간(`beginBackgroundTask`)을 받아 둔다. 뒤로 가면 네트워크가 필요한 백그라운드 처리 작업(BGProcessingTask)을 예약해 이어서 비운다 |

```kotlin
val queue = WorkManagerWorkQueue(context) {
    // DB 에서 보낼 때가 된 일을 비운다
    nextRetryIn // 다시 불러야 할 때까지 남은 시간, 남은 일이 없으면 null
}
queue.wake() // DB 에 일을 넣은 뒤 부른다
```

Android 는 세 번째 인자로 진행 알림을 주면 비우는 동안 데이터 동기화 포그라운드 서비스로 돈다. 비우기를 시작할 때 한 번 불러 구독하고, 알림이 바뀔 때마다 바꿔 단다. 앱이 화면에 없을 때 깨어나 서비스를 시작하지 못하면 알림 없이 비운다.

```kotlin
WorkManagerWorkQueue(context, drain) { progressFlow.map { it?.let(::buildNotification) } }
```

`drain` 이 시간을 돌려주면 그만큼 뒤에 다시 부른다. 예외를 내면 30초 뒤에 다시 부른다. 재시도 횟수와 실패는 앱이 DB 에서 정한다. `queue.online` 은 기기가 지금 연결돼 있는지 알려 준다. 앱은 연결 실패가 기기 탓인지 받는 쪽 탓인지 가를 때 읽는다.

iOS 백그라운드 작업은 식별자를 `Info.plist` 의 `BGTaskSchedulerPermittedIdentifiers` 에 넣고 `UIBackgroundModes` 에 `processing` 을 더해야 한다. `IosWorkQueue` 는 앱이 실행을 마치기 전에 만든다.

## 테스트

Android 구현은 `work-testing` 으로 네트워크 조건과 대기 시간을 채워 호스트 테스트로, iOS 구현은 시뮬레이터 테스트로 확인한다. 백그라운드 처리 작업은 시스템이 정한 때에만 돌아서 자동 테스트가 없다.

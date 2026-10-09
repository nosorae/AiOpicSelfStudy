# work-queue

앱이 꺼지거나 오프라인이어도 남는 작업 대기열이다. 답변 분석 요청을 넣은 순서대로 하나씩 보낸다. 한꺼번에 보내면 무료 키의 분당 한도에 걸리기 때문이다. 프로젝트의 다른 모듈을 의존하지 않는다.

| 플랫폼 | 구현 | 방식 |
|---|---|---|
| Android | `WorkManagerWorkQueue(context, handler)` | 작업 id 를 태그로 단 작업을 고유 사슬 하나에 이어 붙인다. 앞 작업이 끝나야 다음 작업이 돌고, 네트워크가 연결돼야 돈다 |
| iOS | `ForegroundWorkQueue(handler, scope)` | 대기열을 기기에 저장해 두고 앱이 앞에 떠 있고 연결돼 있는 동안 비운다. 못 보낸 것은 다음에 켜질 때 보낸다 |

```kotlin
val queue = WorkManagerWorkQueue(context) { payload ->
    // 보내고 결과를 고른다
    WorkResult.Success // 또는 Retry(429, 408, 5xx, 오프라인), Failure(다시 보내도 안 됨)
}
queue.enqueue(answerId, payload) // 같은 id 가 이미 있으면 넣지 않는다
queue.cancelAll()                // 음성 전송 동의를 철회하면 부른다
```

`Retry` 를 받으면 30초부터 두 배씩 늘려 최대 3번 다시 넘긴다. iOS 는 오프라인인 동안 보내지 않아 재시도 횟수를 쓰지 않는다.

## 테스트

`FakeWorkQueue` 와 그 테스트가 `commonTest` 에 있다. Android 구현은 `work-testing` 으로 네트워크 조건과 재시도 시각을 채워 호스트 테스트로, iOS 구현은 시뮬레이터 테스트로 확인한다.

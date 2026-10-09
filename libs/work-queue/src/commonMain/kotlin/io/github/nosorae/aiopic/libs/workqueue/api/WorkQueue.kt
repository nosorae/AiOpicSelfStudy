package io.github.nosorae.aiopic.libs.workqueue.api

/**
 * 앱이 꺼지거나 오프라인이어도 남는 작업 대기열(F5.1, F5.6). Android 는 WorkManager, iOS 는 앞으로 올 때 비운다.
 * 넣은 순서대로 하나씩 [WorkHandler] 에 넘긴다. 한꺼번에 보내면 무료 키의 분당 한도에 걸린다.
 */
public interface WorkQueue {
    /** 같은 [id] 가 이미 대기열에 있으면 넣지 않는다. 이어 보기로 같은 답변을 다시 넣어도 한 번만 보낸다. */
    public suspend fun enqueue(id: String, payload: String)

    /** 남은 작업을 모두 버린다. 음성 전송 동의를 철회하면 부른다. */
    public suspend fun cancelAll()
}

/** 대기열 구현이 작업 하나를 처리할 때 부른다. 앱이 조립할 때 넘긴다. */
public fun interface WorkHandler {
    public suspend fun handle(payload: String): WorkResult
}

public enum class WorkResult {
    Success,

    /** 429, 408, 5xx, 오프라인처럼 기다리면 될 실패다. 대기열이 지수 백오프로 최대 3번 다시 넘긴다. */
    Retry,

    /** 다시 보내도 안 될 실패다. 작업을 버린다. 녹음과 답변은 남는다. */
    Failure,
}

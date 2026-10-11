package io.github.nosorae.aiopic.libs.workqueue.api

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 앱이 꺼지거나 오프라인이어도 남는 일을 비우는 플랫폼 대기열. 일 자체는 앱의 DB 에 있고, 이 대기열은 [Drain] 을 부르는 때만 정한다.
 * Android 는 WorkManager 고유 작업, iOS 는 앱이 앞에 있을 때와 백그라운드 작업이 부른다. 연결돼 있을 때만 부른다.
 */
public interface WorkQueue {
    /** DB 에 보낼 일이 생겼다. 연결돼 있으면 곧 [Drain.drain] 을 부른다. */
    public fun wake()

    /** 기기가 지금 연결돼 있는가. 비우는 중 연결 실패가 기기 탓인지 받는 쪽 탓인지 가를 때 읽는다. */
    public val online: Boolean
}

/** 앱이 넘기는 일. DB 에서 보낼 때가 된 일을 비우고, 다시 불러야 할 때까지 남은 시간을 돌려준다. 남은 일이 없으면 null 이다. */
public fun interface Drain {
    public suspend fun drain(): Duration?
}

/** [Drain.drain] 이 예외를 내면 이만큼 뒤에 다시 부른다. 대기열이 멈추지 않게 한다. */
internal val RETRY_AFTER_ERROR = 30.seconds

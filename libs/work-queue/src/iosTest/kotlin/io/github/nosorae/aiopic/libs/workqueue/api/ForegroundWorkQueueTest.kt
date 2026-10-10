package io.github.nosorae.aiopic.libs.workqueue.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSUserDefaults
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ForegroundWorkQueueTest {
    private val defaults = NSUserDefaults(suiteName = "work-queue-test")
    private val online = MutableStateFlow(true)
    private val handled = mutableListOf<Pair<String, Long>>()
    private var result = WorkResult.Success

    @BeforeTest
    fun clear() = defaults.removePersistentDomainForName("work-queue-test")

    private fun TestScope.queue(scope: CoroutineScope = backgroundScope) = ForegroundWorkQueue(
        handler = { payload ->
            handled += payload to currentTime
            result
        },
        scope = scope,
        online = online,
        defaults = defaults,
    )

    /** 대기열은 backgroundScope 에서 돌아서 advanceUntilIdle 이 기다려 주지 않는다. 백오프를 다 지나도록 시간을 넘긴다. */
    private fun TestScope.settle() {
        advanceTimeBy(10.minutes)
        runCurrent()
    }

    @Test
    fun `남은 작업 id 를 넣은 순서대로 읽고 보낸 작업은 빠진다`() = runTest {
        online.value = false
        val queue = queue()
        queue.enqueue("a1", "first")
        queue.enqueue("a2", "second")
        settle()

        assertEquals(listOf("a1", "a2"), queue.pending())

        online.value = true
        settle()

        assertEquals(emptyList(), queue.pending())
    }

    @Test
    fun `F5_6 오프라인에서 넣은 작업은 남아 있다가 연결되면 넣은 순서대로 비워진다`() = runTest {
        online.value = false
        val firstRun = CoroutineScope(Job(backgroundScope.coroutineContext[Job]))
        queue(firstRun).apply {
            enqueue("a1", "first")
            enqueue("a2", "second")
        }
        settle()
        assertEquals(emptyList(), handled)

        // 앱이 꺼졌다 다시 켜져도 남아 있다.
        firstRun.cancel()
        queue()
        online.value = true
        settle()

        assertEquals(listOf("first", "second"), handled.map { it.first })
        assertEquals(0, defaults.arrayForKey(QUEUE_KEY)?.size ?: 0)
    }

    @Test
    fun `F5_1 같은 id 를 다시 넣어도 한 번만 보낸다`() = runTest {
        online.value = false
        val queue = queue()
        queue.enqueue("a1", "first")
        queue.enqueue("a1", "again")
        online.value = true
        settle()

        assertEquals(listOf("first"), handled.map { it.first })
    }

    @Test
    fun `F5_6 다시 보낼 실패는 30초부터 두 배씩 기다려 3번 다시 보내고 버린다`() = runTest {
        result = WorkResult.Retry
        val queue = queue()
        queue.enqueue("a1", "first")
        queue.enqueue("a2", "second")
        settle()

        val first = handled.filter { it.first == "first" }.map { it.second }
        assertEquals(listOf(0L, 30, 90, 210).map { it.seconds.inWholeMilliseconds }, first)
        assertEquals(4, handled.count { it.first == "second" })
    }

    @Test
    fun `F1_3 기다리는 중에 모두 취소하면 더 보내지 않는다`() = runTest {
        result = WorkResult.Retry
        val queue = queue()
        queue.enqueue("a1", "first")
        advanceTimeBy(1.seconds)

        queue.cancelAll()
        settle()

        assertEquals(1, handled.size)
    }

    @Test
    fun `F5_6 다시 보내기를 기다리는 사이 끊기면 대기열에 남겼다가 다시 연결되면 보낸다`() = runTest {
        result = WorkResult.Retry
        val queue = queue()
        queue.enqueue("a1", "first")
        advanceTimeBy(10.seconds)

        online.value = false
        settle()
        assertEquals(1, handled.size)
        assertEquals(1, defaults.arrayForKey(QUEUE_KEY)?.size)

        result = WorkResult.Success
        online.value = true
        settle()
        assertEquals(2, handled.size)
        assertEquals(0, defaults.arrayForKey(QUEUE_KEY)?.size ?: 0)
    }
}

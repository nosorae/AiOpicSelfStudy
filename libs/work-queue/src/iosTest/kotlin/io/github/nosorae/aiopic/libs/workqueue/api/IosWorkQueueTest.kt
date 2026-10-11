package io.github.nosorae.aiopic.libs.workqueue.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class IosWorkQueueTest {
    private val online = MutableStateFlow(true)
    private val drains = mutableListOf<Long>()

    /** 부를 때마다 차례로 돌려준다. 다 쓰면 null(남은 작업 없음)이다. */
    private val answers = ArrayDeque<() -> Duration?>()

    private fun TestScope.queue() = IosWorkQueue(
        drain = {
            drains += testScheduler.currentTime
            answers.removeFirstOrNull()?.invoke()
        },
        scope = backgroundScope,
        network = online,
    ).also { runCurrent() }

    @Test
    fun `205 연결되면 앱을 켜자마자 한 번 비운다`() = runTest {
        queue()

        assertEquals(listOf(0L), drains)
    }

    @Test
    fun `205 앱이 앞에 있고 연결돼 있으면 깨울 때 DB 를 비운다`() = runTest {
        val queue = queue()

        queue.wake()
        runCurrent()

        assertEquals(2, drains.size)
    }

    @Test
    fun `205 오프라인이면 깨워도 비우지 않고 연결되면 비운다`() = runTest {
        online.value = false
        val queue = queue()

        queue.wake()
        runCurrent()
        assertEquals(emptyList(), drains)
        assertEquals(false, queue.online)

        online.value = true
        runCurrent()
        assertEquals(1, drains.size)
    }

    @Test
    fun `205 다시 보낼 작업이 남으면 그 시간 뒤에 다시 비우고 없으면 멈춘다`() = runTest {
        answers += { 30.seconds }
        queue()

        advanceTimeBy(10.minutes)
        runCurrent()

        assertEquals(listOf(0L, 30_000L), drains)
    }

    @Test
    fun `205 기다리는 중에 깨우면 바로 비운다`() = runTest {
        answers += { 2.minutes }
        val queue = queue()
        advanceTimeBy(10.seconds)

        queue.wake()
        runCurrent()

        assertEquals(listOf(0L, 10_000L), drains)
    }

    @Test
    fun `205 비우다 예외가 나도 멈추지 않고 나중에 다시 비운다`() = runTest {
        answers += { error("DB 를 못 열었다") }
        queue()

        advanceTimeBy(10.minutes)
        runCurrent()

        assertEquals(listOf(0L, 30_000L), drains)
    }
}

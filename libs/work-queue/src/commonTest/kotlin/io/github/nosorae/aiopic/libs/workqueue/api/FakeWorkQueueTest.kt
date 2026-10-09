package io.github.nosorae.aiopic.libs.workqueue.api

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeWorkQueueTest {
    @Test
    fun `F5_1 넣은 순서대로 남고 같은 id 는 한 번만 들어간다`() = runTest {
        val queue = FakeWorkQueue()

        queue.enqueue("a1", "first")
        queue.enqueue("a2", "second")
        queue.enqueue("a1", "again")

        assertEquals(listOf("a1" to "first", "a2" to "second"), queue.pending)
    }

    @Test
    fun `모두 취소하면 대기열이 빈다`() = runTest {
        val queue = FakeWorkQueue()
        queue.enqueue("a1", "first")

        queue.cancelAll()

        assertTrue(queue.pending.isEmpty())
    }
}

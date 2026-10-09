package io.github.nosorae.aiopic.libs.workqueue.api

import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkManagerWorkQueueTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val handled = mutableListOf<String>()
    private var result = WorkResult.Success
    private val handler = WorkHandler { payload ->
        handled += payload
        result
    }

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
    }

    private fun work(id: String): WorkInfo = WorkManager.getInstance(context).getWorkInfosByTag(id).get().single()

    private fun connect(id: String) = WorkManagerTestInitHelper.getTestDriver(context)!!.setAllConstraintsMet(work(id).id)

    @Test
    fun `F5_6 오프라인에서 넣은 작업은 기다리다 연결되면 넣은 순서대로 비워진다`() = runBlocking {
        val queue = WorkManagerWorkQueue(context, handler)
        queue.enqueue("a1", "first")
        queue.enqueue("a2", "second")

        assertEquals(emptyList(), handled)

        connect("a1")
        connect("a2")

        assertEquals(listOf("first", "second"), handled)
        assertEquals(WorkInfo.State.SUCCEEDED, work("a2").state)
    }

    @Test
    fun `F5_1 같은 id 를 다시 넣어도 한 번만 보낸다`() = runBlocking {
        val queue = WorkManagerWorkQueue(context, handler)
        queue.enqueue("a1", "first")
        queue.enqueue("a1", "again")

        connect("a1")

        assertEquals(listOf("first"), handled)
    }

    @Test
    fun `F1_3 모두 취소하면 연결돼도 보내지 않는다`() = runBlocking {
        val queue = WorkManagerWorkQueue(context, handler)
        queue.enqueue("a1", "first")

        queue.cancelAll()

        assertEquals(WorkInfo.State.CANCELLED, work("a1").state)
        assertEquals(emptyList(), handled)
    }

    @Test
    fun `F5_6 다시 보낼 실패는 3번까지 다시 시도하고 그 뒤에는 버려서 다음 작업을 막지 않는다`() = runBlocking {
        WorkManagerWorkQueue(context, handler)
        result = WorkResult.Retry

        val outcomes = (0..3).map { attempt -> worker(attempt).doWork() }

        assertEquals(List(3) { ListenableWorker.Result.retry() } + ListenableWorker.Result.success(), outcomes)
        assertEquals(4, handled.size)
    }

    @Test
    fun `다시 보내도 안 될 실패는 바로 버리고 다음 작업으로 넘어간다`() = runBlocking {
        WorkManagerWorkQueue(context, handler)
        result = WorkResult.Failure

        assertEquals(ListenableWorker.Result.success(), worker(0).doWork())
    }

    private fun worker(attempt: Int) = TestListenableWorkerBuilder<QueueWorker>(context)
        .setInputData(workDataOf(PAYLOAD to "first"))
        .setRunAttemptCount(attempt)
        .build()
}

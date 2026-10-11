package io.github.nosorae.aiopic.libs.workqueue.api

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkManagerWorkQueueTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private var drains = 0

    /** 부를 때마다 차례로 돌려준다. 다 쓰면 null(남은 작업 없음)이다. */
    private var answers = ArrayDeque<() -> Duration?>()
    private val drain = Drain {
        drains++
        answers.removeFirstOrNull()?.invoke()
    }

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
    }

    private fun works(): List<WorkInfo> = WorkManager.getInstance(context).getWorkInfosForUniqueWork(CHAIN).get().filterNot { it.state.isFinished }

    /** 남은 작업의 네트워크 조건과 대기 시간을 채워 돌린다. */
    private fun runPending() = works().forEach {
        val driver = WorkManagerTestInitHelper.getTestDriver(context)!!
        driver.setAllConstraintsMet(it.id)
        driver.setInitialDelayMet(it.id)
    }

    @Test
    fun `205 깨워도 연결되기 전에는 비우지 않고 연결되면 DB 를 비운다`() {
        val queue = WorkManagerWorkQueue(context, drain)

        queue.wake()
        assertEquals(0, drains)

        runPending()
        assertEquals(1, drains)
        assertEquals(emptyList(), works())
    }

    @Test
    fun `205 다시 보낼 작업이 남으면 그 시간 뒤에 다시 비운다`() {
        WorkManagerWorkQueue(context, drain).wake()
        answers += { 30.seconds }

        runPending()
        assertEquals(1, drains)
        assertEquals(1, works().size)

        runPending()
        assertEquals(2, drains)
        assertEquals(emptyList(), works())
    }

    @Test
    fun `205 비우다 예외가 나도 사슬을 끊지 않고 나중에 다시 비운다`() {
        WorkManagerWorkQueue(context, drain).wake()
        answers += { error("DB 를 못 열었다") }

        runPending()
        assertEquals(1, works().size)

        runPending()
        assertEquals(2, drains)
    }

    @Test
    fun `205 여러 번 깨워도 뒤 작업이 이미 있으면 다시 비우기를 겹쳐 넣지 않는다`() {
        val queue = WorkManagerWorkQueue(context, drain)
        queue.wake()
        queue.wake()
        answers += { 30.seconds }
        answers += { 30.seconds }

        runPending()

        // 깨운 두 번이 차례로 돌고, 뒤 작업이 있던 첫 번째는 다시 비우기를 넣지 않는다.
        assertEquals(2, drains)
        assertEquals(1, works().size)
    }
}

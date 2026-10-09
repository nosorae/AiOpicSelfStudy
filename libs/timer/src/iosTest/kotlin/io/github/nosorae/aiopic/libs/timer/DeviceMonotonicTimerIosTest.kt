package io.github.nosorae.aiopic.libs.timer

import platform.posix.usleep
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class DeviceMonotonicTimerIosTest {
    @Test
    fun elapsedMovesForwardWithRealTime() {
        val timer = deviceMonotonicTimer()
        val start = timer.elapsed()

        usleep(50_000u)

        val passed = timer.elapsed() - start
        assertTrue(passed >= 50.milliseconds && passed < 1000.milliseconds, "흐른 시간 $passed")
    }
}

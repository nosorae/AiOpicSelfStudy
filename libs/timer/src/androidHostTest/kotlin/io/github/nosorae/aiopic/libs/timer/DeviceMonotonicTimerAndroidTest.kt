package io.github.nosorae.aiopic.libs.timer

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/** F2.5: 40분 타이머는 기기가 잠든 동안에도 흐르는 부팅 이후 시간으로 잰다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeviceMonotonicTimerAndroidTest {
    @Test
    fun elapsedFollowsBootClock() {
        val timer = deviceMonotonicTimer()
        val start = timer.elapsed()

        ShadowSystemClock.advanceBy(5.seconds.toJavaDuration())

        assertEquals(5.seconds, timer.elapsed() - start)
    }
}

package io.github.nosorae.aiopic.ui.design

import androidx.compose.runtime.State
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

/** Next 채움과 다시 듣기 창이 쓰는 흐른 비율. 1초마다 오는 값 사이를 끊김 없이 채우고, 동작 줄이기면 받은 값 그대로인지 본다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ElapsedFractionTest {
    @get:Rule val rule = createComposeRule()

    private fun fraction(reduceMotion: Boolean): State<Float> {
        lateinit var state: State<Float>
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MontageTheme(reduceMotion = reduceMotion) { state = rememberElapsedFraction(elapsed = 2.seconds, total = 10.seconds, running = true) }
        }
        rule.mainClock.advanceTimeByFrame()
        return state
    }

    @Test
    fun `흐르는 중이면 받은 시간에서 시작해 남은 시간 동안 1 까지 고르게 찬다`() {
        val state = fraction(reduceMotion = false)

        rule.mainClock.advanceTimeBy(4_000)
        assertEquals(0.6f, state.value, 0.02f)

        rule.mainClock.advanceTimeBy(5_000)
        assertEquals(1f, state.value, 0f)
    }

    @Test
    fun `동작 줄이기면 시간이 흘러도 받은 값에 멈춰 있다`() {
        val state = fraction(reduceMotion = true)

        rule.mainClock.advanceTimeBy(4_000)

        assertEquals(0.2f, state.value, 0f)
    }
}

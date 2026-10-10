package io.github.nosorae.aiopic.libs.debug

import kotlin.test.Test
import kotlin.test.assertEquals

class ShakeDetectorTest {
    private val detector = ShakeDetector()
    private val earth = 9.81f

    @Test
    fun aHardShakeOpensOnceUntilTheCooldownPasses() {
        val shakes = listOf(0L, 300L, 1_200L).count { detector.isShake(3 * earth, 0f, earth, it) }

        assertEquals(2, shakes)
    }

    @Test
    fun walkingAroundIsNotAShake() {
        assertEquals(false, detector.isShake(earth, earth / 2, earth, 0L))
    }
}

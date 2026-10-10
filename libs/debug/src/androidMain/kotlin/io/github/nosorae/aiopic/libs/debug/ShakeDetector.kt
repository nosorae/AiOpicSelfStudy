package io.github.nosorae.aiopic.libs.debug

import kotlin.math.sqrt

/** 가속도 한 번이 중력의 [threshold] 배를 넘으면 흔들기로 본다. 한 번 흔드는 동안 여러 번 열리지 않게 [cooldownMillis] 를 둔다. */
internal class ShakeDetector(private val threshold: Float = 2.7f, private val cooldownMillis: Long = 1_000) {
    private var last = Long.MIN_VALUE / 2

    fun isShake(x: Float, y: Float, z: Float, nowMillis: Long): Boolean {
        val g = sqrt(x * x + y * y + z * z) / EARTH_GRAVITY
        if (g < threshold || nowMillis - last < cooldownMillis) return false
        last = nowMillis
        return true
    }

    private companion object {
        const val EARTH_GRAVITY = 9.81f
    }
}

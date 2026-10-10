package io.github.nosorae.aiopic.ui.design

import kotlin.test.Test
import kotlin.test.assertEquals

class MontageWaveformTest {
    @Test
    fun `음량이 크면 막대가 높고 조용해도 막대가 남는다`() {
        assertEquals(listOf(0.08f, 0.5f, 1f), waveformHeights(listOf(0f, 0.5f, 1.4f), reduceMotion = false))
    }

    @Test
    fun `동작 줄이기면 음량과 상관없이 같은 높이로 멈춘다`() {
        assertEquals(waveformHeights(listOf(0f, 0f), true), waveformHeights(listOf(0.9f, 0.2f), true))
    }
}

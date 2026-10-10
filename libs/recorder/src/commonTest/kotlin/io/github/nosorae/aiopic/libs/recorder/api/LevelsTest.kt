package io.github.nosorae.aiopic.libs.recorder.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelsTest {
    @Test
    fun `가장 큰 소리는 1 이고 무음은 0 이고 그 사이는 0과 1 사이다`() {
        assertEquals(1f, loudness(1f))
        assertEquals(0f, loudness(0f))
        listOf(0.001f, 0.01f, 0.1f, 0.5f).forEach { assertTrue(loudness(it) in 0f..1f, "$it") }
    }

    @Test
    fun `범위를 벗어난 진폭도 0과 1 사이로 자른다`() {
        assertEquals(1f, loudness(3f))
        assertEquals(0f, loudness(-1f))
        assertEquals(0f, loudness(0.000_01f))
    }

    @Test
    fun `보통 말소리 크기에서 띠가 눈에 띄게 움직이도록 로그로 바꾼다`() {
        // 선형 그대로면 최대의 10% 진폭은 0.1 이라 띠가 거의 안 부푼다.
        assertTrue(loudness(0.1f) > 0.5f, "${loudness(0.1f)}")
        assertTrue(loudness(0.01f) < loudness(0.1f))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `음량은 80ms 마다 하나씩 나온다`() = runTest {
        val values = levelFlow { 1f }.take(3).toList()

        assertEquals(listOf(1f, 1f, 1f), values)
        assertEquals(160, currentTime)
    }

    @Test
    fun `가짜 녹음기는 녹음 중일 때만 넣은 진폭을 음량으로 낸다`() = runTest {
        val recorder = FakeRecorder()
        recorder.peak = 1f
        assertEquals(0f, recorder.levels.first())

        recorder.start("a1")
        assertEquals(1f, recorder.levels.first())

        recorder.stop()
        assertEquals(0f, recorder.levels.first())
    }
}

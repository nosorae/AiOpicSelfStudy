package io.github.nosorae.aiopic.libs.recorder.api

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class FakeRecorderTest {
    @Test
    fun `F3_3 다시 켜서 같은 폴더에 이어 녹음하면 조각 번호가 이어진다`() = runTest {
        val recorder = FakeRecorder()
        recorder.start("a1")
        recorder.stop()

        recorder.start("a1")
        val chunks = recorder.stop().chunks

        assertEquals(listOf("a1/0.m4a", "a1/1.m4a"), chunks)
        assertEquals(chunks, recorder.chunks("a1"))
    }

    @Test
    fun `F3_4 이어 녹음을 시작한 위치를 합친 녹음 기준으로 돌려준다`() = runTest {
        val recorder = FakeRecorder(chunkLength = 5.seconds)
        recorder.start("a1")
        recorder.stop()
        recorder.start("a1")
        val chunks = recorder.stop().chunks

        assertEquals(listOf(5.seconds), recorder.merge(chunks, "a1.m4a"))
    }

    @Test
    fun `마이크를 열 수 없으면 시작하지 못한다`() = runTest {
        val recorder = FakeRecorder(micAvailable = false)

        assertEquals(RecordingFailure.MicUnavailable, recorder.start("a1"))
    }

    @Test
    fun `조각 쓰기에 실패하면 멈출 때 알린다`() = runTest {
        val recorder = FakeRecorder(writeFails = true)
        recorder.start("a1")

        assertEquals(RecordingFailure.WriteFailed, recorder.stop().failure)
    }
}

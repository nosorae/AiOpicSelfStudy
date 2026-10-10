package io.github.nosorae.aiopic.libs.recorder.api

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
class IosRecorderTest {
    private val dir = NSTemporaryDirectory() + "recorder-" + NSUUID().UUIDString
    private val mic = FakeMic()

    @AfterTest
    fun cleanUp() {
        NSFileManager.defaultManager.removeItemAtPath(dir, null)
    }

    @Test
    fun `F3_2 조각 길이를 채우면 녹음기를 멈추지 않고 새 조각 파일에 이어 쓴다`() = runTest {
        val recorder = IosRecorder(mic, chunkLength = 1.seconds)
        assertNull(recorder.start(dir))
        mic.feed(2500.milliseconds)
        val chunks = recorder.stop().chunks

        assertEquals(listOf("0000.m4a", "0001.m4a", "0002.m4a"), chunks.map { it.substringAfterLast('/') })
        assertEquals(1, mic.starts)
        assertEquals(emptyList(), recorder.merge(chunks, "$dir/answer.m4a"))
    }

    @Test
    fun `F3_4 끼어듦이 끝나면 새 조각부터 다시 쓰고 그 위치를 합친 녹음 기준으로 돌려준다`() = runTest {
        val recorder = IosRecorder(mic, chunkLength = 5.seconds)
        recorder.start(dir)
        mic.feed(1500.milliseconds)

        postInterruption(AVAudioSessionInterruptionTypeBegan)
        mic.feed(1.seconds) // 끼어든 동안 들어온 소리는 쓰지 않는다
        postInterruption(AVAudioSessionInterruptionTypeEnded)
        mic.feed(1.seconds)
        val chunks = recorder.stop().chunks

        assertEquals(listOf("0000.m4a", "0001-resume.m4a"), chunks.map { it.substringAfterLast('/') })
        assertEquals(2, mic.starts)
        assertNear(listOf(1500.milliseconds), recorder.merge(chunks, "$dir/answer.m4a"))
    }

    @Test
    fun `F3_3 앱이 죽었다 켜지면 남은 조각 목록을 되찾고 그 뒤 번호부터 이어 녹음한다`() = runTest {
        val before = IosRecorder(mic, chunkLength = 1.seconds)
        before.start(dir)
        mic.feed(1500.milliseconds)
        // 앱이 죽으면 stop 이 불리지 않는다. 쓰던 조각은 닫히지 않아 읽을 수 없다.

        val after = IosRecorder(mic, chunkLength = 1.seconds)
        assertEquals(listOf("0000.m4a", "0001.m4a"), after.chunks(dir).map { it.substringAfterLast('/') })

        after.start(dir)
        mic.feed(500.milliseconds)
        val chunks = after.stop().chunks

        assertEquals(listOf("0000.m4a", "0001.m4a", "0002-resume.m4a"), chunks.map { it.substringAfterLast('/') })
        assertNear(listOf(1.seconds), after.merge(chunks, "$dir/answer.m4a"))
    }

    @Test
    fun `읽을 수 없는 조각은 건너뛰고 합친다`() = runTest {
        val recorder = IosRecorder(mic, chunkLength = 5.seconds)
        recorder.start(dir)
        mic.feed(1.seconds)
        val chunks = recorder.stop().chunks
        NSData().writeToFile("$dir/0001.m4a", atomically = true)

        assertEquals(emptyList(), recorder.merge(chunks + "$dir/0001.m4a", "$dir/answer.m4a"))
        assertTrue(NSFileManager.defaultManager.fileExistsAtPath("$dir/answer.m4a"))
    }

    @Test
    fun `마이크를 열 수 없으면 시작하지 못한다`() = runTest {
        assertEquals(RecordingFailure.MicUnavailable, IosRecorder(FakeMic(available = false), chunkLength = 5.seconds).start(dir))
    }

    @Test
    fun `폴더를 만들 수 없으면 시작하지 못한다`() = runTest {
        NSData().writeToFile(dir, atomically = true)

        assertEquals(RecordingFailure.WriteFailed, IosRecorder(mic, chunkLength = 5.seconds).start("$dir/answer"))
    }

    @Test
    fun `조각 파일을 만들지 못하면 멈출 때 알린다`() = runTest {
        val recorder = IosRecorder(mic, chunkLength = 1.seconds)
        recorder.start(dir)
        mic.feed(1500.milliseconds)
        // 녹음 도중 폴더가 사라지면 다음 조각 파일을 만들 수 없다.
        NSFileManager.defaultManager.removeItemAtPath(dir, null)
        mic.feed(1.seconds)

        assertEquals(RecordingFailure.WriteFailed, recorder.stop().failure)
    }

    @Test
    fun `끼어듦이 끝난 뒤 마이크를 다시 열지 못하면 멈출 때 알린다`() = runTest {
        val recorder = IosRecorder(mic, chunkLength = 5.seconds)
        recorder.start(dir)
        mic.feed(1.seconds)
        postInterruption(AVAudioSessionInterruptionTypeBegan)
        mic.available = false
        postInterruption(AVAudioSessionInterruptionTypeEnded)
        val stopped = recorder.stop()

        assertEquals(RecordingFailure.MicUnavailable, stopped.failure)
        assertEquals(listOf("0000.m4a"), stopped.chunks.map { it.substringAfterLast('/') })
    }

    @Test
    fun `실패는 다음 녹음으로 넘어가지 않는다`() = runTest {
        val recorder = IosRecorder(mic, chunkLength = 5.seconds)
        recorder.start(dir)
        postInterruption(AVAudioSessionInterruptionTypeBegan)
        mic.available = false
        postInterruption(AVAudioSessionInterruptionTypeEnded)
        recorder.stop()
        mic.available = true

        recorder.start(dir)
        mic.feed(1.seconds)

        assertNull(recorder.stop().failure)
    }

    @Test
    fun `음량은 직전 값 뒤로 들어온 가장 큰 소리이고 녹음 중이 아니면 0 이다`() = runTest {
        val recorder = IosRecorder(mic, chunkLength = 5.seconds)
        mic.feed(100.milliseconds, sample = 0.5f)
        assertEquals(0f, recorder.levels.first())

        recorder.start(dir)
        mic.feed(100.milliseconds, sample = 0.1f)
        mic.feed(100.milliseconds, sample = -0.5f)
        assertEquals(loudness(0.5f), recorder.levels.first())
        // 그 뒤로 소리가 들어오지 않았다.
        assertEquals(0f, recorder.levels.first())

        mic.feed(100.milliseconds, sample = 0.5f)
        recorder.stop()
        assertEquals(0f, recorder.levels.first())
    }

    @Test
    fun `폴더가 없으면 조각 목록은 비어 있다`() = runTest {
        assertEquals(emptyList(), IosRecorder(mic, chunkLength = 5.seconds).chunks(dir))
    }

    private fun postInterruption(type: ULong) {
        NSNotificationCenter.defaultCenter.postNotificationName(
            AVAudioSessionInterruptionNotification,
            null,
            mapOf<Any?, Any?>(AVAudioSessionInterruptionTypeKey to NSNumber(unsignedLongLong = type)),
        )
    }

    /** AAC 는 1024 프레임 묶음으로 쓰여서 길이가 몇 밀리초 다를 수 있다. */
    private fun assertNear(expected: List<Duration>, actual: List<Duration>) {
        assertEquals(expected.size, actual.size, "$actual")
        expected.zip(actual).forEach { (e, a) -> assertTrue((e - a).absoluteValue < 50.milliseconds, "$e, $a") }
    }
}

/** 마이크 대신 16kHz 모노 버퍼를 0.1초씩 넘긴다. 표본은 모두 [feed] 의 sample 이고 기본은 무음이다. 열려 있지 않으면 버퍼를 넘기지 않는다. */
@OptIn(ExperimentalForeignApi::class)
private class FakeMic(var available: Boolean = true) : MicInput {
    private val format = AVAudioFormat(standardFormatWithSampleRate = 16_000.0, channels = 1u)
    private var onBuffer: ((AVAudioPCMBuffer) -> Unit)? = null
    var starts = 0

    override fun start(onBuffer: (AVAudioPCMBuffer) -> Unit): Boolean {
        if (!available) return false
        starts++
        this.onBuffer = onBuffer
        return true
    }

    override fun stop() {
        onBuffer = null
    }

    fun feed(length: Duration, sample: Float = 0f) {
        repeat((length / 100.milliseconds).toInt()) {
            val buffer = AVAudioPCMBuffer(format, frameCapacity = 1_600u)
            buffer.frameLength = 1_600u
            val samples = buffer.floatChannelData!![0]!!
            repeat(1_600) { samples[it] = sample }
            onBuffer?.invoke(buffer)
        }
    }
}

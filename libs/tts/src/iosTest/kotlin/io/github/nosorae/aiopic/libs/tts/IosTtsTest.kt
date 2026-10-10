package io.github.nosorae.aiopic.libs.tts

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** F2.3: 시뮬레이터의 실제 음성 엔진으로 읽혀 끝 알림이 오는지 본다. 알림이 메인 런루프로 오므로 테스트가 런루프를 돌린다. */
@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class IosTtsTest {
    private val tts = IosTts()

    @Test
    fun speakReturnsTrueAfterSpeechFinishes() {
        val spoken = speakOnMain("Hello.")

        runMainLoop(30.seconds) { spoken.isCompleted }

        assertTrue(spoken.isCompleted, "30초 안에 끝 알림이 오지 않았다")
        assertEquals(true, spoken.getCompleted())
    }

    /** #134: 녹음 전 첫 읽기도 무음 스위치에 묻히지 않는 녹음 겸용 카테고리여야 한다. 기본 카테고리(SoloAmbient)는 무음 모드에서 소리가 안 난다. */
    @Test
    fun speakUsesPlayAndRecordCategoryBeforeAnyRecording() {
        val spoken = speakOnMain("Hi.")

        assertEquals(AVAudioSessionCategoryPlayAndRecord, AVAudioSession.sharedInstance().category)
        runMainLoop(30.seconds) { spoken.isCompleted }
    }

    @Test
    fun speakReturnsFalseWhenStopped() {
        val spoken = speakOnMain("Tell me about the park you like to visit, and what you usually do there.")

        tts.stop()
        runMainLoop(5.seconds) { spoken.isCompleted }

        assertTrue(spoken.isCompleted)
        assertEquals(false, spoken.getCompleted())
    }

    // 첫 중단 지점까지 바로 돌려서, speak 가 문장을 넘긴 뒤에 stop 을 부르게 한다.
    private fun speakOnMain(text: String): Deferred<Boolean> = GlobalScope.async(Dispatchers.Main, CoroutineStart.UNDISPATCHED) { tts.speak(text) }

    private fun runMainLoop(limit: Duration, until: () -> Boolean) {
        val deadline = TimeSource.Monotonic.markNow() + limit
        while (!until() && deadline.hasNotPassedNow()) {
            NSRunLoop.currentRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.02))
        }
    }
}

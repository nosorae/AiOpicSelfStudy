package io.github.nosorae.aiopic.libs.tts

import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** F2.3: 질문을 다 읽으면 speak 가 true 로 돌아와야 녹음이 저절로 시작된다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidTtsTest {
    private val tts = AndroidTts(ApplicationProvider.getApplicationContext())
    private val engine get() = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())

    @Test
    fun speakReturnsTrueWhenEngineReportsDone() = runTest {
        val spoken = async { tts.speak("Tell me about yourself.") }
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        runCurrent()
        assertEquals("Tell me about yourself.", engine.lastSpokenText)

        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(spoken.await())
    }

    @Test
    fun speakReturnsFalseWhenStopped() = runTest {
        val spoken = async { tts.speak("Tell me about yourself.") }
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        runCurrent()

        tts.stop()

        assertFalse(spoken.await())
        assertTrue(engine.isStopped)
    }

    @Test
    fun speakReturnsFalseWhenEngineFailsToStart() = runTest {
        val spoken = async { tts.speak("Tell me about yourself.") }
        engine.onInitListener.onInit(TextToSpeech.ERROR)

        assertFalse(spoken.await())
    }

    /** #161: 미리 만든 음성을 틀지 못하면 시험이 멈추지 않게 기기 음성으로 읽는다. */
    @Test
    fun speakFallsBackToDeviceVoiceWhenAudioCannotPlay() = runTest {
        val spoken = async { tts.speak("Tell me about yourself.", audio = byteArrayOf(1, 2, 3)) }
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        runCurrent()
        assertEquals("Tell me about yourself.", engine.lastSpokenText)

        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(spoken.await())
    }
}

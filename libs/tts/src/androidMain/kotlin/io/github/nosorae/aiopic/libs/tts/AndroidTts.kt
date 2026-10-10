package io.github.nosorae.aiopic.libs.tts

import android.content.Context
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

/**
 * 미리 만든 음성을 [MediaPlayer] 로 틀거나 Android 기기 음성으로 질문을 읽는다. 음성 엔진은 만들 때 비동기로 켜지므로 첫 [speak] 는 켜질 때까지 기다린다.
 * 앱이 떠 있는 동안 하나를 계속 쓴다.
 */
// ponytail: 엔진을 shutdown 하지 않는다. 화면마다 만들게 되면 닫는 함수를 더한다.
public class AndroidTts(context: Context) : Tts {
    private val ready = CompletableDeferred<Boolean>()
    private val engine = TextToSpeech(context.applicationContext) { ready.complete(it == TextToSpeech.SUCCESS) }
    private val nextId = AtomicInteger()

    /** 지금 읽는 문장의 id 와 그 끝을 기다리는 호출. 앞 문장의 늦은 알림이 새 문장을 끝내지 않게 id 를 맞춰 본다. */
    private val pending = AtomicReference<Pair<String, CancellableContinuation<Boolean>>?>()

    init {
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) = Unit

                override fun onDone(utteranceId: String) = finish(utteranceId, true)

                @Deprecated("API 21 부터 onError(String, Int) 가 같이 불린다")
                override fun onError(utteranceId: String) = finish(utteranceId, false)

                override fun onStop(utteranceId: String, interrupted: Boolean) = finish(utteranceId, false)
            },
        )
    }

    /** 지금 트는 음성 파일과 그 끝을 기다리는 호출. */
    private val playing = AtomicReference<Pair<MediaPlayer, CancellableContinuation<Boolean>>?>()

    override suspend fun speak(text: String, audio: ByteArray?): Boolean {
        if (audio != null) play(audio)?.let { return it }
        // 미리 만든 음성을 못 틀었을 때를 기기 로그로 볼 수 있게 남긴다. 직접 조작 확인도 이 줄을 본다.
        Log.i(TAG, "기기 음성으로 읽는다 (음성 파일 ${if (audio == null) "없음" else "못 틂"})")
        if (!ready.await()) return false
        chooseVoiceOnce()
        val id = "q${nextId.incrementAndGet()}"
        return suspendCancellableCoroutine { cont ->
            pending.getAndSet(id to cont)?.second?.resume(false)
            cont.invokeOnCancellation { engine.stop() }
            if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) finish(id, false)
        }
    }

    /** 틀지 못하는 파일이면 null 이다. 그러면 기기 음성으로 읽는다. */
    private suspend fun play(audio: ByteArray): Boolean? {
        val player = MediaPlayer()
        try {
            player.setDataSource(BytesSource(audio))
            player.prepare()
        } catch (_: Exception) {
            player.release()
            return null
        }
        Log.i(TAG, "음성 파일을 튼다 (${audio.size}바이트)")
        return suspendCancellableCoroutine { cont ->
            playing.getAndSet(player to cont)?.let { endPlay(it.first, false) }
            player.setOnCompletionListener { endPlay(player, true) }
            player.setOnErrorListener { _, _, _ ->
                endPlay(player, false)
                true
            }
            cont.invokeOnCancellation { endPlay(player, false) }
            player.start()
        }
    }

    private fun endPlay(player: MediaPlayer, done: Boolean) {
        val current = playing.get() ?: return
        if (current.first !== player || !playing.compareAndSet(current, null)) return
        player.release()
        if (current.second.isActive) current.second.resume(done)
    }

    override fun hasHighQualityVoice(): Boolean {
        // 엔진이 아직 안 켜졌으면 안내하지 않는다. 설정 화면을 다시 열면 그때 본다.
        if (!ready.isCompleted) return true
        return installedUsVoices().any { it.quality >= Voice.QUALITY_HIGH }
    }

    override fun stop() {
        playing.get()?.let { endPlay(it.first, false) }
        engine.stop()
        pending.getAndSet(null)?.second?.resume(false)
    }

    private fun finish(id: String, spoken: Boolean) {
        val current = pending.get() ?: return
        if (current.first == id && pending.compareAndSet(current, null)) current.second.resume(spoken)
    }

    private var voiceChosen = false

    /** 설치된 en-US 음성 가운데 기본 음성보다 품질이 높은 것이 있으면 그것으로 읽는다(설계 8절). */
    private fun chooseVoiceOnce() {
        if (voiceChosen) return
        voiceChosen = true
        engine.setLanguage(Locale.US)
        val best = bestVoice(installedUsVoices(), { true }, { it.quality }) ?: return
        if (best.quality > (engine.voice?.quality ?: 0)) engine.setVoice(best)
    }

    private fun installedUsVoices(): List<Voice> = engine.voices.orEmpty()
        .filter { it.locale == Locale.US && !it.isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
}

private const val TAG = "AndroidTts"

/** 파일로 쓰지 않고 메모리의 음성을 바로 튼다. */
private class BytesSource(private val data: ByteArray) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= data.size) return -1
        val count = minOf(size.toLong(), data.size - position).toInt()
        data.copyInto(buffer, offset, position.toInt(), position.toInt() + count)
        return count
    }

    override fun getSize(): Long = data.size.toLong()

    override fun close() = Unit
}

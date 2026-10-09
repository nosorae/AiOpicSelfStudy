package io.github.nosorae.aiopic.libs.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

/**
 * Android 기기 음성으로 질문을 읽는다. 음성 엔진은 만들 때 비동기로 켜지므로 첫 [speak] 는 켜질 때까지 기다린다.
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

    override suspend fun speak(text: String): Boolean {
        if (!ready.await()) return false
        chooseVoiceOnce()
        val id = "q${nextId.incrementAndGet()}"
        return suspendCancellableCoroutine { cont ->
            pending.getAndSet(id to cont)?.second?.resume(false)
            cont.invokeOnCancellation { engine.stop() }
            if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) finish(id, false)
        }
    }

    override fun stop() {
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
        val best = engine.voices
            ?.filter { it.locale == Locale.US && !it.isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
            ?.maxByOrNull { it.quality }
            ?: return
        if (best.quality > (engine.voice?.quality ?: 0)) engine.setVoice(best)
    }
}

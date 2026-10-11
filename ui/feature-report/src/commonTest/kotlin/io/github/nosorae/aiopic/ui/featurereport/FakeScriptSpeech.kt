package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.coroutines.CompletableDeferred

/** 받은 글과 음성을 남기고, [finish] 를 부를 때까지 읽기를 끝내지 않는 기기 음성. 취소되면 읽기를 멈춘 것으로 센다. */
class FakeTts : Tts {
    val spoken = mutableListOf<Pair<String, ByteArray?>>()
    private var done = CompletableDeferred<Boolean>()

    override suspend fun speak(text: String, audio: ByteArray?): Boolean {
        spoken += text to audio
        return done.await()
    }

    fun finish() {
        done.complete(true)
        done = CompletableDeferred()
    }

    override fun hasHighQualityVoice() = false

    override fun stop() = Unit
}

/** [audio] 를 돌려주는 자연스러운 음성. 부를 때마다 글을 남기고, [ready] 가 끝날 때까지 돌려주지 않는다. */
class FakeScriptVoice(private val audio: ByteArray? = byteArrayOf(1), val ready: CompletableDeferred<Unit> = CompletableDeferred(Unit)) : ScriptVoice {
    val asked = mutableListOf<String>()

    override suspend fun synthesize(text: String): ByteArray? {
        asked += text
        ready.await()
        return audio
    }
}

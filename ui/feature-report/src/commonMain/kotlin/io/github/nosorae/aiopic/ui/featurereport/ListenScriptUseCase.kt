package io.github.nosorae.aiopic.ui.featurereport

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 최종 스크립트를 읽는 음성(#217). 설정이 자연스러운 음성이면 Gemini 음성을 받아 오고, 꺼졌거나 키가 없거나 받지 못하면 null 이다. 앱 층이 잇는다. */
fun interface ScriptVoice {
    suspend fun synthesize(text: String): ByteArray?
}

/** 최종 스크립트를 듣는 버튼의 상태(#217). 자연스러운 음성을 받는 몇 초 동안 [Preparing] 이다. */
enum class ScriptPlayback { Idle, Preparing, Playing }

/**
 * 최종 스크립트를 소리 내 읽는다(#217). 자연스러운 음성을 받는 동안 [ScriptPlayback.Preparing], 읽는 동안 [ScriptPlayback.Playing],
 * 다 읽으면 [ScriptPlayback.Idle] 을 낸다. 받지 못하면 기기 음성으로 읽는다. 구독을 끊으면 읽기도 멈춘다.
 */
@Inject
class ListenScriptUseCase(private val voice: ScriptVoice, private val tts: Tts) {
    operator fun invoke(script: String): Flow<ScriptPlayback> = flow {
        emit(ScriptPlayback.Preparing)
        val audio = voice.synthesize(script)
        emit(ScriptPlayback.Playing)
        tts.speak(script, audio)
        emit(ScriptPlayback.Idle)
    }
}

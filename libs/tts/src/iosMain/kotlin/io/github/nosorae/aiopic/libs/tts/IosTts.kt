package io.github.nosorae.aiopic.libs.tts

import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesisVoiceQualityDefault
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * iOS 기기 음성으로 질문을 읽는다. 끝 알림은 메인 스레드로 오므로 [speak] 와 [stop] 도 메인 스레드에서 부른다.
 * 오디오 세션 설정은 녹음과 함께 정해야 해서 여기서 건드리지 않는다.
 */
public class IosTts : Tts {
    private val synthesizer = AVSpeechSynthesizer()

    /** 지금 읽는 문장과 그 끝을 기다리는 호출. 앞 문장의 늦은 알림이 새 문장을 끝내지 않게 문장을 맞춰 본다. */
    private var pending: Pair<AVSpeechUtterance, CancellableContinuation<Boolean>>? = null

    // 합성기는 delegate 를 약하게 잡으므로 여기서 들고 있는다.
    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) = finish(didFinishSpeechUtterance, true)

        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didCancelSpeechUtterance: AVSpeechUtterance) = finish(didCancelSpeechUtterance, false)
    }

    /** 설치된 en-US 음성 가운데 고품질(Enhanced, Premium)이 있으면 그것을, 없으면 기본 음성을 쓴다(설계 8절). */
    private val voice: AVSpeechSynthesisVoice? by lazy {
        AVSpeechSynthesisVoice.speechVoices()
            .filterIsInstance<AVSpeechSynthesisVoice>()
            .filter { it.language == "en-US" && it.quality != AVSpeechSynthesisVoiceQualityDefault }
            .maxByOrNull { it.quality }
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
    }

    init {
        synthesizer.delegate = delegate
    }

    override suspend fun speak(text: String): Boolean = suspendCancellableCoroutine { cont ->
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text).also { it.voice = voice }
        pending?.second?.resume(false)
        pending = utterance to cont
        cont.invokeOnCancellation { stop() }
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        val current = pending ?: return
        pending = null
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        if (current.second.isActive) current.second.resume(false)
    }

    private fun finish(utterance: AVSpeechUtterance, spoken: Boolean) {
        val current = pending ?: return
        if (current.first != utterance) return
        pending = null
        current.second.resume(spoken)
    }
}

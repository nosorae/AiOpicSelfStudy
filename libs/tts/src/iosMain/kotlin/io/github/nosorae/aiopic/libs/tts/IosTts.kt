package io.github.nosorae.aiopic.libs.tts

import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesisVoiceQualityDefault
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.Foundation.NSLog
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * iOS 기기 음성으로 질문을 읽는다. 끝 알림은 메인 스레드로 오므로 [speak] 와 [stop] 도 메인 스레드에서 부른다.
 *
 * 읽기 전에 오디오 세션 카테고리를 녹음기(`EngineMicInput`)와 같은 녹음 겸용, 스피커 출력으로 맞춘다.
 * 앱 기본 카테고리(SoloAmbient)는 무음 모드에서 소리가 안 나서, 녹음을 한 번도 안 한 첫 질문만 들리지 않았다(#134).
 * 세션 활성화는 합성기가 읽을 때 스스로 한다. 앱을 켜자마자 활성화하면 다른 앱의 음악이 끊긴다.
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
        useSpeakerCategory()
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

    @OptIn(ExperimentalForeignApi::class)
    private fun useSpeakerCategory() {
        val session = AVAudioSession.sharedInstance()
        if (session.category == AVAudioSessionCategoryPlayAndRecord) return
        val set = session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            mode = AVAudioSessionModeDefault,
            options = AVAudioSessionCategoryOptionDefaultToSpeaker,
            error = null,
        )
        // 실패해도 읽기는 한다. 무음 모드가 아니면 들린다. 실기기에서만 나는 일이라 원인을 볼 수 있게 남긴다.
        if (!set) NSLog("IosTts: 오디오 세션 카테고리를 바꾸지 못했다 (지금 %@)", session.category)
    }

    private fun finish(utterance: AVSpeechUtterance, spoken: Boolean) {
        val current = pending ?: return
        if (current.first != utterance) return
        pending = null
        current.second.resume(spoken)
    }
}

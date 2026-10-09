package io.github.nosorae.aiopic.libs.recorder.api

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.setActive

/** 마이크에서 받은 소리를 버퍼로 넘긴다. 테스트에서 마이크 없이 버퍼를 넣으려고 둔 경계다. */
internal interface MicInput {
    /** 마이크를 열고 받은 버퍼를 [onBuffer] 로 넘긴다. 열지 못하면 false 다. */
    fun start(onBuffer: (AVAudioPCMBuffer) -> Unit): Boolean

    fun stop()
}

/**
 * AVAudioEngine 입력에 탭을 걸어 버퍼를 받는다. 조각을 바꿀 때 엔진을 멈추지 않아 소리가 빠지지 않는다.
 * 화면이 꺼져도 이어지려면 앱 Info.plist 의 UIBackgroundModes 에 audio 가 있어야 한다(F3.5).
 */
@OptIn(ExperimentalForeignApi::class)
internal class EngineMicInput : MicInput {
    private val engine = AVAudioEngine()

    override fun start(onBuffer: (AVAudioPCMBuffer) -> Unit): Boolean {
        val session = AVAudioSession.sharedInstance()
        val ready = session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            mode = AVAudioSessionModeDefault,
            options = AVAudioSessionCategoryOptionDefaultToSpeaker,
            error = null,
        ) && session.setActive(true, error = null)
        if (!ready) return false
        val input = engine.inputNode
        val format = input.outputFormatForBus(0u)
        // 마이크 권한이 없거나 입력 장치가 없으면 표본율이 0 이다.
        if (format.sampleRate == 0.0) return false
        input.removeTapOnBus(0u)
        input.installTapOnBus(0u, bufferSize = 4_096u, format = format) { buffer, _ -> buffer?.let(onBuffer) }
        engine.prepare()
        return engine.startAndReturnError(null)
    }

    override fun stop() {
        engine.inputNode.removeTapOnBus(0u)
        engine.stop()
    }
}

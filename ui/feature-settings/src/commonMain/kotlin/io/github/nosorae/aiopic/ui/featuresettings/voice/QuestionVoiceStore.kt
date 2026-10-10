package io.github.nosorae.aiopic.ui.featuresettings.voice

/**
 * 질문을 무엇으로 읽을지 기기에 남긴다(#161). 기본은 자연스러운 음성(Gemini 로 미리 만든 음성)이다.
 * 실제 저장과 기기 음성 확인은 앱 층이 넣고, 테스트는 메모리 가짜를 쓴다.
 */
interface QuestionVoiceStore {
    suspend fun naturalVoiceOn(): Boolean

    suspend fun setNaturalVoiceOn(on: Boolean)

    /** 영어(미국) 기기 음성 가운데 기본보다 좋은 음성이 설치돼 있는지. 없으면 기기 음성을 고를 때 받는 곳을 안내한다. */
    fun hasHighQualityDeviceVoice(): Boolean
}

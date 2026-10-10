package io.github.nosorae.aiopic.libs.tts.api

/**
 * 질문을 읽는다. 미리 만든 음성이 있으면 그 파일을 틀고, 없거나 틀지 못하면 기기 음성으로 읽는다(#161).
 * 기기 음성은 설치된 것 가운데 가장 좋은 것(iOS Premium, Enhanced, 기본 순)을 고른다.
 * 실제 시험처럼 질문이 끝나면 녹음이 저절로 시작돼야 해서 다 읽을 때까지 돌아오지 않는다.
 */
public interface Tts {
    /**
     * [audio](WAV 나 m4a 파일 내용)가 있으면 틀고, 없거나 틀지 못하면 [text] 를 기기 음성으로 읽는다.
     * 끝까지 읽었으면 true, [stop] 으로 멈췄거나 읽지 못했으면 false 다. false 면 녹음을 시작하지 않는다.
     */
    public suspend fun speak(text: String, audio: ByteArray? = null): Boolean

    /** 설치된 영어(미국) 기기 음성 가운데 기본보다 좋은 음성(Enhanced, Premium)이 있는지. 설정 화면이 받는 경로를 안내할지 정한다. */
    public fun hasHighQualityVoice(): Boolean

    public fun stop()
}

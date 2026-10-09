package io.github.nosorae.aiopic.libs.tts.api

/**
 * 질문을 기기 음성으로 읽는다(설계 8절). 고품질 음성이 설치돼 있으면 그것을 고른다.
 * 실제 시험처럼 질문이 끝나면 녹음이 저절로 시작돼야 해서 다 읽을 때까지 돌아오지 않는다.
 */
public interface Tts {
    /** 끝까지 읽었으면 true, [stop] 으로 멈췄거나 읽지 못했으면 false 다. false 면 녹음을 시작하지 않는다. */
    public suspend fun speak(text: String): Boolean

    public fun stop()
}

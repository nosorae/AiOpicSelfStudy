package io.github.nosorae.aiopic.libs.tts.api

/** 테스트용. 소리 없이 바로 끝까지 읽은 것으로 하고 읽은 글을 [spoken] 에 남긴다. */
public class FakeTts : Tts {
    public val spoken: MutableList<String> = mutableListOf()

    override suspend fun speak(text: String): Boolean {
        spoken += text
        return true
    }

    override fun stop(): Unit = Unit
}

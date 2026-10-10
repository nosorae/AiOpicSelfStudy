package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.libs.tts.api.Tts

/**
 * 질문 음성(#161). 설정이 자연스러운 음성(기본)이면 미리 만든 Gemini 음성을 틀고, 없으면 [device] 의 기기 음성으로 읽는다.
 * 내장 문항은 시험 팩에 넣은 파일([bundled])이고, 모델이 낸 문항은 출제를 받을 때 [prepare] 가 받아 [directory] 에 둔 파일이다.
 * 파일 이름이 질문 글에서 나와서 이어 볼 때도 같은 음성을 다시 쓴다. 화면은 이 클래스를 [Tts] 로만 안다.
 * [prepare] 는 미리 받는 요청과 같은 메인 스레드에서 부른다.
 */
// ponytail: 받은 음성은 Gemini 가 주는 WAV(질문 하나에 약 300KB) 그대로 둔다. 회차가 끝나면 [clean] 이 지워서 진행 중인 회차 하나 몫(약 4.7MB)만 남는다.
// 기기에서 AAC 로 줄이면 플랫폼마다 인코더 코드가 생긴다. 진행 중인 회차를 여럿 두게 되면 그때 줄인다.
class QuestionVoice(
    private val device: Tts,
    private val naturalVoiceOn: () -> Boolean,
    private val pool: suspend () -> List<Question>,
    private val bundled: suspend (String) -> ByteArray?,
    private val directory: String,
    /** 글을 읽은 WAV. 키가 없거나 받지 못하면 null 이다. */
    private val synthesize: suspend (String) -> ByteArray?,
    private val read: (String) -> ByteArray? = ::readFileOrNull,
    private val write: (String, ByteArray) -> Unit = ::writeFile,
    private val list: (String) -> List<String> = ::listFiles,
    private val delete: (String) -> Unit = ::deleteFile,
) : Tts {
    private val preparing = mutableSetOf<String>()

    override suspend fun speak(text: String, audio: ByteArray?): Boolean = device.speak(text, audio ?: if (naturalVoiceOn()) find(text) else null)

    override fun hasHighQualityVoice(): Boolean = device.hasHighQualityVoice()

    override fun stop() = device.stop()

    /** 모델이 낸 질문의 음성을 받아 둔다. 내장 문항이거나, 이미 받았거나, 받는 중이면 부르지 않는다. 못 받으면 Play 때 기기 음성으로 읽는다. */
    suspend fun prepare(question: Question) {
        val path = path(question.text)
        if (!naturalVoiceOn() || pool().any { it.text == question.text } || read(path) != null || !preparing.add(path)) return
        try {
            synthesize(question.text)?.let { write(path, it) }
        } finally {
            preparing.remove(path)
        }
    }

    /**
     * [keep] 질문의 음성만 남기고 [directory] 의 파일을 모두 지운다(#185). 회차가 끝날 때 진행 중인 회차의 질문을 넘긴다.
     * 끝난 회차의 질문, 고르지 않은 재조정 선택지의 첫 질문, 회차가 끝난 뒤에 받기를 마친 음성이 함께 지워진다.
     */
    fun clean(keep: List<String>) {
        val kept = keep.map(::path).toSet()
        list(directory).filterNot { it in kept }.forEach(delete)
    }

    private suspend fun find(text: String): ByteArray? = pool().firstOrNull { it.text == text }?.let { bundled(it.id) } ?: read(path(text))

    // ponytail: 32비트 해시에 글자 수를 붙여 이름을 짓는다. 질문 1,000개가 쌓여도 두 질문이 겹칠 확률은 0.01% 남짓이다. 겹치면 다른 질문 음성이 나오므로 질문이 훨씬 많아지면 SHA-256 으로 바꾼다.
    private fun path(text: String) = "$directory/${text.hashCode().toUInt().toString(16)}-${text.length}.wav"
}

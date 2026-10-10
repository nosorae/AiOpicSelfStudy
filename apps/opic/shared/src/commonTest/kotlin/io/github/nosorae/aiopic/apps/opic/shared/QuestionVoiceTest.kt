package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** #161: 질문은 미리 만든 자연스러운 음성으로 읽고, 없으면 기기 음성으로 읽는다. */
class QuestionVoiceTest {
    private val builtIn = Question("self-1", "Tell me about yourself.", QuestionType("self-intro"), "self")
    private val generated = Question("s1-2", "Describe your favorite park.", QuestionType("description"), "parks")

    private val files = mutableMapOf<String, ByteArray>()
    private val synthesized = mutableListOf<String>()
    private var natural = true
    private var synthesis: ByteArray? = byteArrayOf(9)
    private val device = AudioLog()

    private val voice = QuestionVoice(
        device = device,
        naturalVoiceOn = { natural },
        pool = { listOf(builtIn) },
        bundled = { id -> if (id == builtIn.id) byteArrayOf(1) else null },
        directory = "voices",
        synthesize = { text ->
            synthesized += text
            synthesis
        },
        read = { files[it] },
        write = { path, bytes -> files[path] = bytes },
        list = { dir -> files.keys.filter { it.startsWith("$dir/") } },
        delete = { files.remove(it) },
    )

    @Test
    fun `내장 문항은 앱에 넣은 음성을 튼다`() = runTest {
        voice.speak(builtIn.text)

        assertContentEquals(byteArrayOf(1), device.audio.single())
    }

    @Test
    fun `모델이 낸 문항은 미리 받아 둔 음성을 튼다`() = runTest {
        voice.prepare(generated)
        voice.speak(generated.text)

        assertEquals(listOf(generated.text), synthesized)
        assertContentEquals(byteArrayOf(9), device.audio.single())
    }

    @Test
    fun `이미 받았거나 내장 문항이면 다시 받지 않는다`() = runTest {
        voice.prepare(generated)
        voice.prepare(generated)
        voice.prepare(builtIn)

        assertEquals(listOf(generated.text), synthesized)
    }

    @Test
    fun `받지 못했으면 기기 음성으로 읽는다`() = runTest {
        synthesis = null
        voice.prepare(generated)
        voice.speak(generated.text)

        assertNull(device.audio.single())
        assertEquals(listOf(generated.text), device.texts)
    }

    @Test
    fun `기기 음성을 고르면 음성 파일을 틀지도 받지도 않는다`() = runTest {
        natural = false
        voice.prepare(generated)
        voice.speak(builtIn.text)

        assertEquals(emptyList(), synthesized)
        assertNull(device.audio.single())
    }

    @Test
    fun `185 남길 질문 말고 받아 둔 음성을 모두 지운다`() = runTest {
        val other = generated.copy(id = "s1-3", text = "Tell me about your last trip.")
        voice.prepare(generated)
        voice.prepare(other)
        files["voices/leftover.wav.partial"] = byteArrayOf(0)

        voice.clean(keep = listOf(other.text))
        voice.speak(generated.text)
        voice.speak(other.text)

        assertEquals(1, files.size)
        assertEquals(listOf(null, byteArrayOf(9).toList()), device.audio.map { it?.toList() })
    }

    /** 받은 글과 음성을 모은다. 음성이 null 이면 기기 음성으로 읽은 것이다. */
    class AudioLog : Tts {
        val texts = mutableListOf<String>()
        val audio = mutableListOf<ByteArray?>()

        override suspend fun speak(text: String, audio: ByteArray?): Boolean {
            texts += text
            this.audio += audio
            return true
        }

        override fun hasHighQualityVoice() = true

        override fun stop() = Unit
    }
}

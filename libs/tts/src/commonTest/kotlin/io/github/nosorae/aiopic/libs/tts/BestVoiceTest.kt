package io.github.nosorae.aiopic.libs.tts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** #161: 기기 음성은 설치된 것 가운데 가장 좋은 것을 고른다. 품질 값은 iOS 와 같게 기본 1, Enhanced 2, Premium 3 이다. */
class BestVoiceTest {
    private data class Voice(val name: String, val language: String, val quality: Int)

    private fun pick(vararg voices: Voice) = bestVoice(voices.toList(), { it.language == "en-US" }, { it.quality })?.name

    @Test
    fun `Premium 이 Enhanced 와 기본보다 앞선다`() {
        assertEquals("Ava", pick(Voice("Samantha", "en-US", 1), Voice("Evan", "en-US", 2), Voice("Ava", "en-US", 3), Voice("Daniel", "en-GB", 3)))
    }

    @Test
    fun `고품질이 없으면 기본 음성을 고른다`() {
        assertEquals("Samantha", pick(Voice("Daniel", "en-GB", 3), Voice("Samantha", "en-US", 1)))
    }

    @Test
    fun `영어 미국 음성이 없으면 고르지 않는다`() {
        assertNull(pick(Voice("Daniel", "en-GB", 3)))
    }
}

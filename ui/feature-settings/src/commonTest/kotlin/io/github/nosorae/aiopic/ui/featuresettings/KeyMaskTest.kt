package io.github.nosorae.aiopic.ui.featuresettings

import io.github.nosorae.aiopic.ui.featuresettings.key.maskKey
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyMaskTest {
    @Test
    fun `긴 키는 앞 3자와 끝 4자만 남긴다`() {
        assertEquals("tes…wxyz", maskKey("test-key-0123456789-wxyz"))
    }

    @Test
    fun `가려도 남는 글자가 없는 짧은 키는 통째로 가린다`() {
        assertEquals("…", maskKey("1234567"))
    }
}

package io.github.nosorae.aiopic.ui.featuresettings.info

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LicenseFileTest {
    private val bundled = File("src/commonMain/composeResources/$LICENSES_FILE").readText()

    @Test
    fun `앱에 싣는 고지 문서는 저장소 루트의 THIRD_PARTY_LICENSES 와 같다`() {
        assertEquals(File("../../THIRD_PARTY_LICENSES.md").readText(), bundled, "루트 파일을 고쳤으면 같은 내용으로 복사한다")
    }

    @Test
    fun `라이선스 원문은 줄바꿈을 그대로 둔 한 덩어리로 싣고 제목과 본문 줄을 나눈다`() {
        val blocks = licenseBlocks(bundled)

        assertEquals(LicenseBlock.Heading("제3자 라이선스"), blocks.first())
        val mit = blocks.filterIsInstance<LicenseBlock.Verbatim>().first()
        assertTrue(mit.text.startsWith("MIT License\n\nCopyright (c) 2025 Wanted Lab, Inc."), mit.text)
        assertTrue(blocks.none { "```" in it.text })
    }
}

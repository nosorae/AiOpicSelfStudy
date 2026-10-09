package io.github.nosorae.aiopic.libs.keyvault.api

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeKeyVaultTest {
    @Test
    fun `넣은 키를 같은 제공사 이름으로 읽는다`() = runTest {
        val vault = FakeKeyVault()

        vault.put("gemini", "test-key")

        assertEquals("test-key", vault.get("gemini"))
    }

    @Test
    fun `F1_2 한 제공사 키를 지워도 다른 제공사 키는 남는다`() = runTest {
        val vault = FakeKeyVault()
        vault.put("gemini", "test-key")
        vault.put("other", "other-key")

        vault.delete("gemini")

        assertNull(vault.get("gemini"))
        assertEquals("other-key", vault.get("other"))
    }
}

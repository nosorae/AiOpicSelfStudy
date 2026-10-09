package io.github.nosorae.aiopic.libs.keyvault.api

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 모든 KeyVault 구현이 지켜야 할 동작(F1.2). 플랫폼 구현은 이 클래스를 상속해 실제 저장소에서 돌린다.
 * Android 계측 테스트에서도 돌기 때문에 테스트 이름에 공백 대신 밑줄을 쓴다. 최소 API 28 의 DEX 는 이름에 공백을 받지 않는다.
 */
abstract class KeyVaultContractTest {
    abstract fun createVault(): KeyVault

    private val vault by lazy { createVault() }

    // 실제 저장소는 테스트가 끝나도 남으므로 쓴 제공사 키를 지운다.
    @AfterTest
    fun cleanUp() = runTest {
        vault.delete(GEMINI)
        vault.delete(OTHER)
    }

    @Test
    fun `넣은_키를_같은_제공사_이름으로_읽는다`() = runTest {
        vault.put(GEMINI, "test-key")

        assertEquals("test-key", vault.get(GEMINI))
    }

    @Test
    fun `저장된_키가_없으면_null_이다`() = runTest {
        assertNull(vault.get(GEMINI))
    }

    @Test
    fun `같은_제공사_키를_다시_넣으면_바뀐다`() = runTest {
        vault.put(GEMINI, "test-key")

        vault.put(GEMINI, "test-key-2")

        assertEquals("test-key-2", vault.get(GEMINI))
    }

    @Test
    fun `F1_2_한_제공사_키를_지워도_다른_제공사_키는_남는다`() = runTest {
        vault.put(GEMINI, "test-key")
        vault.put(OTHER, "other-key")

        vault.delete(GEMINI)

        assertNull(vault.get(GEMINI))
        assertEquals("other-key", vault.get(OTHER))
    }

    @Test
    fun `없는_키를_지워도_실패하지_않는다`() = runTest {
        vault.delete(GEMINI)

        assertNull(vault.get(GEMINI))
    }

    private companion object {
        const val GEMINI = "gemini"
        const val OTHER = "other"
    }
}

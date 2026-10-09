package io.github.nosorae.aiopic.ui.featureonboarding.key

import io.github.nosorae.aiopic.ui.featureonboarding.DomainError
import io.github.nosorae.aiopic.ui.featureonboarding.DomainResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SaveKeyUseCaseTest {
    private val vault = FakeKeyVault()
    private val saveKeyUseCase = SaveKeyUseCase(vault, acceptOnly("good-key"))

    @Test
    fun `F1_1 확인을 통과한 키는 제공사 이름으로 저장한다`() = runTest {
        saveKeyUseCase(KeyProvider.Gemini, "good-key")

        assertEquals("good-key", vault.get("gemini"))
    }

    @Test
    fun `F1_1 틀린 키는 저장하지 않고 이유를 돌려준다`() = runTest {
        val result = saveKeyUseCase(KeyProvider.Gemini, "bad-key")

        assertEquals(DomainResult.Failure(DomainError.InvalidKey), result)
        assertNull(vault.get("gemini"))
    }

    @Test
    fun `붙여 넣을 때 딸려 온 앞뒤 공백은 떼고 확인한다`() = runTest {
        saveKeyUseCase(KeyProvider.Gemini, "  good-key\n")

        assertEquals("good-key", vault.get("gemini"))
    }
}

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

    @Test
    fun `마크다운에서 복사하며 딸려 온 역슬래시는 빼고 키 글자는 그대로 둔다`() {
        assertEquals("AQ.abc-def_9", cleanKey("AQ\\.abc\\-def_9"))
    }

    @Test
    fun `공백과 줄바꿈과 눈에 안 보이는 문자는 어디에 있든 뺀다`() {
        assertEquals("good-key", cleanKey("\uFEFF good\u200B-\u2060key\n\t"))
    }

    @Test
    fun `저장할 때도 정리한 키로 확인하고 저장한다`() = runTest {
        saveKeyUseCase(KeyProvider.Gemini, "good\\-key\u200D")

        assertEquals("good-key", vault.get("gemini"))
    }
}

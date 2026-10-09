package io.github.nosorae.aiopic.ui.featureonboarding.start

import io.github.nosorae.aiopic.ui.featureonboarding.consent.FakeVoiceConsent
import io.github.nosorae.aiopic.ui.featureonboarding.key.FakeKeyVault
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DecideStartUseCaseTest {
    private val vault = FakeKeyVault()
    private val consent = FakeVoiceConsent()
    private val decideStartUseCase = DecideStartUseCase(vault, consent)

    @Test
    fun `F1_0 키가 없으면 키 입력으로 간다`() = runTest {
        consent.given = true

        assertEquals(StartDestination.KeyEntry, decideStartUseCase())
    }

    @Test
    fun `F1_0 키는 있고 동의 전이면 동의 화면으로 간다`() = runTest {
        vault.put("gemini", "test-key")

        assertEquals(StartDestination.Consent, decideStartUseCase())
    }

    @Test
    fun `F1_0 키와 동의가 다 있으면 홈으로 간다`() = runTest {
        vault.put("gemini", "test-key")
        consent.given = true

        assertEquals(StartDestination.Home, decideStartUseCase())
    }

    @Test
    fun `F1_3 동의 전에는 마이크 권한을 묻는 홈 이후로 가지 않는다`() = runTest {
        vault.put("gemini", "test-key")

        // 마이크 권한(S6)은 홈에서 시험이나 연습을 시작할 때 묻는다. 동의 전에는 홈에 닿지 않는다.
        assertNotEquals(StartDestination.Home, decideStartUseCase())
        consent.give()
        assertEquals(StartDestination.Home, decideStartUseCase())
    }
}

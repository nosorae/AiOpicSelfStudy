package io.github.nosorae.aiopic.ui.featuresettings

import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider
import io.github.nosorae.aiopic.ui.featureonboarding.start.DecideStartUseCase
import io.github.nosorae.aiopic.ui.featureonboarding.start.StartDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 설정에서 음성 전송 동의를 철회한다(F1.3). */
class ConsentRevokeTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeSettingsStore()
    private val vault = FakeKeyVault(KeyProvider.Gemini.id to "test-key")
    private val queue = FakeWorkQueue("a1", "a2")

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `F1_3 철회를 확인하면 동의를 지우고 대기 중인 전송을 버리고 다음 실행은 동의 화면으로 간다`() = runTest(dispatcher) {
        val viewModel = settingsViewModel(store, vault, queue)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.consentGiven)

        viewModel.onIntent(SettingsIntent.RequestConsentRevoke)
        assertTrue(viewModel.uiState.value.revokingConsent)
        viewModel.onIntent(SettingsIntent.RevokeConsent)
        advanceUntilIdle()

        assertFalse(store.consentGiven)
        assertEquals(emptyList(), queue.pending)
        assertFalse(viewModel.uiState.value.consentGiven)
        assertFalse(viewModel.uiState.value.revokingConsent)
        assertEquals(StartDestination.Consent, DecideStartUseCase(vault, store)())
    }

    @Test
    fun `철회를 취소하면 동의와 대기열이 그대로다`() = runTest(dispatcher) {
        val viewModel = settingsViewModel(store, vault, queue)
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.RequestConsentRevoke)
        viewModel.onIntent(SettingsIntent.CancelConsentRevoke)
        advanceUntilIdle()

        assertTrue(store.consentGiven)
        assertEquals(listOf("a1", "a2"), queue.pending)
        assertFalse(viewModel.uiState.value.revokingConsent)
    }

    @Test
    fun `동의 화면에서 다시 동의하고 돌아오면 동의한 상태로 보인다`() = runTest(dispatcher) {
        store.consentGiven = false
        val viewModel = settingsViewModel(store, vault, queue)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.consentGiven)

        store.give()
        viewModel.onIntent(SettingsIntent.Reload)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.consentGiven)
    }
}

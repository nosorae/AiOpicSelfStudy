package io.github.nosorae.aiopic.ui.featureonboarding.start

import io.github.nosorae.aiopic.ui.featureonboarding.consent.FakeVoiceConsent
import io.github.nosorae.aiopic.ui.featureonboarding.key.FakeKeyVault
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

class StartViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `F1_0 갈 곳을 정할 때까지 시스템 스플래시를 잡아 두고 정하면 놓는다`() = runTest(dispatcher) {
        val viewModel = StartViewModel(DecideStartUseCase(FakeKeyVault(), FakeVoiceConsent()))

        assertTrue(viewModel.uiState.value.keepSplash)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.keepSplash)
        assertEquals(StartDestination.KeyEntry, viewModel.uiState.value.destination)
    }
}

package io.github.nosorae.aiopic.ui.featureonboarding.consent

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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConsentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val consent = FakeVoiceConsent()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ConsentViewModel(GiveVoiceConsentUseCase(consent))

    @Test
    fun `F1_3 동의 버튼을 누르기 전에는 동의가 아니다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        // 뒤로가기나 홈으로 나가면 Intent 없이 화면만 사라진다. 그때도 동의는 저장되지 않아 전송이 막힌다.
        assertFalse(consent.isGiven())
        assertNull(viewModel.uiState.value.navigation)
    }

    @Test
    fun `F1_3 동의 버튼을 누르면 저장하고 다음으로 간다`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(ConsentIntent.GiveConsent)
        advanceUntilIdle()

        assertTrue(consent.isGiven())
        assertEquals(ConsentDestination.ConsentGiven, viewModel.uiState.value.navigation)
    }

    @Test
    fun `화면이 이동을 처리했다고 알리면 이동 필드를 지운다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onIntent(ConsentIntent.GiveConsent)
        advanceUntilIdle()

        viewModel.onIntent(ConsentIntent.NavigationHandled)

        assertNull(viewModel.uiState.value.navigation)
    }
}

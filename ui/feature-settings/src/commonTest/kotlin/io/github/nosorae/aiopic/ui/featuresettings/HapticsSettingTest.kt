package io.github.nosorae.aiopic.ui.featuresettings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 진동 켜기/끄기. 끄면 재생하지 않는 쪽은 앱 틀의 `AppSettingsTest` 가 본다. */
class HapticsSettingTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeSettingsStore()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `진동은 기본으로 켜져 있다`() = runTest(dispatcher) {
        val viewModel = settingsViewModel(store, FakeKeyVault())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hapticsOn)
    }

    @Test
    fun `끄면 저장되고 다시 켤 수 있다`() = runTest(dispatcher) {
        val viewModel = settingsViewModel(store, FakeKeyVault())
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.SetHaptics(on = false))
        advanceUntilIdle()
        assertFalse(store.hapticsOn)
        assertFalse(viewModel.uiState.value.hapticsOn)

        viewModel.onIntent(SettingsIntent.SetHaptics(on = true))
        advanceUntilIdle()
        assertTrue(store.hapticsOn)
        assertTrue(viewModel.uiState.value.hapticsOn)
    }
}

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

/** #161: 질문 음성은 자연스러운 음성이 기본이고 기기 음성으로 바꿀 수 있다. 무엇을 트는지는 앱 틀의 `QuestionVoiceTest` 가 본다. */
class QuestionVoiceSettingTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeSettingsStore()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `자연스러운 음성이 기본이다`() = runTest(dispatcher) {
        val viewModel = settingsViewModel(store, FakeKeyVault())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.naturalVoiceOn)
        assertFalse(viewModel.uiState.value.showsDeviceVoiceGuide)
    }

    @Test
    fun `기기 음성을 고르면 저장되고 고품질 음성이 없으면 받는 곳을 안내한다`() = runTest(dispatcher) {
        store.highQualityDeviceVoice = false
        val viewModel = settingsViewModel(store, FakeKeyVault())
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.SetNaturalVoice(on = false))
        advanceUntilIdle()

        assertFalse(store.naturalVoiceOn)
        assertFalse(viewModel.uiState.value.naturalVoiceOn)
        assertTrue(viewModel.uiState.value.showsDeviceVoiceGuide)
    }

    @Test
    fun `고품질 기기 음성이 있으면 안내하지 않는다`() = runTest(dispatcher) {
        val viewModel = settingsViewModel(store, FakeKeyVault())
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.SetNaturalVoice(on = false))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showsDeviceVoiceGuide)
    }
}

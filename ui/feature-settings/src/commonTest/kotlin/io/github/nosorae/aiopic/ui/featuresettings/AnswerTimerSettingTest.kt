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

/** 문항별 답변 타이머 켜기(F2.7). 시험 화면이 타이머를 보이는 쪽은 `ExamViewModelTest` 가 본다. */
class AnswerTimerSettingTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeSettingsStore()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `F2_7 답변 타이머는 기본으로 꺼져 있다`() = runTest(dispatcher) {
        val viewModel = settingsViewModel(store, FakeKeyVault())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.answerTimerOn)
    }

    @Test
    fun `F2_7 켜면 저장되고 끄면 다시 꺼진다`() = runTest(dispatcher) {
        val viewModel = settingsViewModel(store, FakeKeyVault())
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.SetAnswerTimer(on = true))
        advanceUntilIdle()
        assertTrue(store.answerTimerOn)
        assertTrue(viewModel.uiState.value.answerTimerOn)

        viewModel.onIntent(SettingsIntent.SetAnswerTimer(on = false))
        advanceUntilIdle()
        assertFalse(store.answerTimerOn)
        assertFalse(viewModel.uiState.value.answerTimerOn)
    }

    @Test
    fun `켜 둔 값은 설정을 다시 열어도 남는다`() = runTest(dispatcher) {
        store.answerTimerOn = true

        val viewModel = settingsViewModel(store, FakeKeyVault())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.answerTimerOn)
    }
}

package io.github.nosorae.aiopic.ui.featuresettings

import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider
import io.github.nosorae.aiopic.ui.featuresettings.key.DeleteKeyUseCase
import io.github.nosorae.aiopic.ui.featuresettings.key.KeyStatus
import io.github.nosorae.aiopic.ui.featuresettings.model.SelectModelUseCase
import io.github.nosorae.aiopic.ui.featuresettings.theme.SetThemeModeUseCase
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeSettingsStore()
    private val vault = FakeKeyVault()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.loaded(): SettingsViewModel {
        val viewModel = SettingsViewModel(
            loadSettingsUseCase = LoadSettingsUseCase(FakeCatalog, store, store, vault),
            selectModelUseCase = SelectModelUseCase(store),
            deleteKeyUseCase = DeleteKeyUseCase(vault),
            setThemeModeUseCase = SetThemeModeUseCase(store),
        )
        advanceUntilIdle()
        return viewModel
    }

    private fun SettingsViewModel.send(intent: SettingsIntent) {
        onIntent(intent)
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `F5_8 앱이 들고 있는 목록을 보이고 고르지 않았으면 기본 모델이 골라져 있다`() = runTest(dispatcher) {
        val state = loaded().uiState.value

        assertEquals(FakeCatalog.models, state.models)
        assertEquals(FakeCatalog.stable.id, state.selectedModelId)
        assertNull(state.fellBackFrom)
    }

    @Test
    fun `F5_8 모델을 고르면 저장된다`() = runTest(dispatcher) {
        val viewModel = loaded()

        viewModel.send(SettingsIntent.SelectModel(FakeCatalog.otherStable.id))

        assertEquals(FakeCatalog.otherStable.id, store.modelId)
        assertEquals(FakeCatalog.otherStable.id, viewModel.uiState.value.selectedModelId)
    }

    @Test
    fun `F5_8 미리보기 모델을 고르면 2주 공지 안내가 보이고 안정 모델이면 안 보인다`() = runTest(dispatcher) {
        val viewModel = loaded()
        assertFalse(viewModel.uiState.value.showsPreviewNotice)

        viewModel.send(SettingsIntent.SelectModel(FakeCatalog.preview.id))

        assertTrue(viewModel.uiState.value.showsPreviewNotice)
    }

    @Test
    fun `F5_8 고른 모델이 목록에서 빠졌으면 기본 모델로 돌려 저장하고 알린다`() = runTest(dispatcher) {
        store.modelId = "removed-model"

        val state = loaded().uiState.value

        assertEquals(FakeCatalog.stable.id, state.selectedModelId)
        assertEquals(FakeCatalog.stable.id, store.modelId)
        assertEquals("removed-model", state.fellBackFrom)
    }

    @Test
    fun `기본 모델로 돌린 알림은 모델을 다시 고르면 사라진다`() = runTest(dispatcher) {
        store.modelId = "removed-model"
        val viewModel = loaded()

        viewModel.send(SettingsIntent.SelectModel(FakeCatalog.otherStable.id))

        assertNull(viewModel.uiState.value.fellBackFrom)
    }

    @Test
    fun `F1_2 제공사마다 키가 저장돼 있는지 보인다`() = runTest(dispatcher) {
        vault.keys[KeyProvider.Gemini.id] = "test-key"

        val state = loaded().uiState.value

        assertEquals(listOf(KeyStatus(KeyProvider.Gemini, saved = true)), state.keys)
    }

    @Test
    fun `F1_2 키 지우기를 확인하면 그 제공사 키만 지우고 다른 제공사 키는 남는다`() = runTest(dispatcher) {
        vault.keys[KeyProvider.Gemini.id] = "test-key"
        vault.keys["other"] = "other-key"
        val viewModel = loaded()

        viewModel.send(SettingsIntent.RequestKeyDeletion(KeyProvider.Gemini))
        assertEquals(KeyProvider.Gemini, viewModel.uiState.value.deletingKey)
        viewModel.send(SettingsIntent.DeleteKey)

        assertEquals(mapOf("other" to "other-key"), vault.keys)
        assertEquals(listOf(KeyStatus(KeyProvider.Gemini, saved = false)), viewModel.uiState.value.keys)
        assertNull(viewModel.uiState.value.deletingKey)
    }

    @Test
    fun `키 지우기를 취소하면 키가 남는다`() = runTest(dispatcher) {
        vault.keys[KeyProvider.Gemini.id] = "test-key"
        val viewModel = loaded()

        viewModel.send(SettingsIntent.RequestKeyDeletion(KeyProvider.Gemini))
        viewModel.send(SettingsIntent.CancelKeyDeletion)

        assertEquals("test-key", vault.keys[KeyProvider.Gemini.id])
        assertNull(viewModel.uiState.value.deletingKey)
    }

    @Test
    fun `키 입력 화면에서 돌아와 다시 읽으면 새로 저장한 키가 보이고 기본 모델 알림은 남는다`() = runTest(dispatcher) {
        store.modelId = "removed-model"
        val viewModel = loaded()
        vault.keys[KeyProvider.Gemini.id] = "test-key"

        viewModel.send(SettingsIntent.Reload)

        assertTrue(viewModel.uiState.value.keys.single().saved)
        assertEquals("removed-model", viewModel.uiState.value.fellBackFrom)
    }

    @Test
    fun `F9_1 화면 모드 기본값은 시스템 따라가기다`() = runTest(dispatcher) {
        assertEquals(ThemeMode.System, loaded().uiState.value.themeMode)
    }

    @Test
    fun `F9_1 화면 모드를 고르면 저장되고 앱 틀이 보는 값이 바로 바뀐다`() = runTest(dispatcher) {
        val viewModel = loaded()

        viewModel.send(SettingsIntent.SelectThemeMode(ThemeMode.Dark))

        assertEquals(ThemeMode.Dark, store.themeMode.value)
        assertEquals(ThemeMode.Dark, viewModel.uiState.value.themeMode)
    }
}

package io.github.nosorae.aiopic.ui.featureonboarding.key

import io.github.nosorae.aiopic.ui.featureonboarding.DomainError
import io.github.nosorae.aiopic.ui.featureonboarding.DomainResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyEntryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val vault = FakeKeyVault()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(provider: KeyProvider? = null, checker: KeyChecker = acceptOnly("good-key")) = KeyEntryViewModel(provider, SaveKeyUseCase(vault, checker))

    @Test
    fun `F1_1 제공사 목록에는 Gemini 하나만 보인다`() {
        val viewModel = viewModel()

        assertEquals(listOf(KeyProvider.Gemini), viewModel.uiState.value.providers)
    }

    @Test
    fun `제공사가 하나뿐이면 정하지 않고 들어와도 그 제공사가 골라져 있다`() {
        val viewModel = viewModel()

        assertEquals(KeyProvider.Gemini, viewModel.uiState.value.provider)
    }

    @Test
    fun `제공사가 둘 이상일 때 정하지 않고 들어오면 아무 제공사도 골라져 있지 않다`() {
        // 첫 버전은 제공사가 하나라 같은 값을 둘 넣어 개수 규칙만 본다.
        val state = KeyEntryUiState(providers = List(2) { KeyProvider.Gemini })

        assertNull(state.provider)
        assertFalse(state.copy(key = "good-key").canSubmit)
    }

    @Test
    fun `화면 인자로 제공사를 받으면 그 제공사가 골라져 있다`() {
        val viewModel = viewModel(provider = KeyProvider.Gemini)

        assertEquals(KeyProvider.Gemini, viewModel.uiState.value.provider)
    }

    @Test
    fun `키를 넣어야 보낼 수 있다`() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onIntent(KeyEntryIntent.ChangeKey("good-key"))

        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `제공사를 정하지 않고 들어와도 하나뿐인 제공사로 키를 저장한다`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(KeyEntryIntent.ChangeKey("good-key"))
        viewModel.onIntent(KeyEntryIntent.SubmitKey)
        advanceUntilIdle()

        assertEquals("good-key", vault.get("gemini"))
    }

    @Test
    fun `F1_1 맞는 키를 보내면 저장하고 다음 화면으로 간다`() = runTest(dispatcher) {
        val viewModel = viewModel(provider = KeyProvider.Gemini)

        viewModel.onIntent(KeyEntryIntent.ChangeKey("good-key"))
        viewModel.onIntent(KeyEntryIntent.SubmitKey)
        advanceUntilIdle()

        assertEquals(KeyEntryDestination.KeySaved, viewModel.uiState.value.navigation)
        assertEquals("good-key", vault.get("gemini"))
    }

    @Test
    fun `F1_1 틀린 키를 보내면 이유를 보여 주고 이동하지 않는다`() = runTest(dispatcher) {
        val viewModel = viewModel(provider = KeyProvider.Gemini)

        viewModel.onIntent(KeyEntryIntent.ChangeKey("bad-key"))
        viewModel.onIntent(KeyEntryIntent.SubmitKey)
        advanceUntilIdle()

        assertEquals(DomainError.InvalidKey, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.navigation)
    }

    @Test
    fun `확인하는 동안에는 다시 보낼 수 없다`() = runTest(dispatcher) {
        val answer = CompletableDeferred<DomainResult<Unit>>()
        val viewModel = viewModel(provider = KeyProvider.Gemini, checker = { _, _ -> answer.await() })

        viewModel.onIntent(KeyEntryIntent.ChangeKey("good-key"))
        viewModel.onIntent(KeyEntryIntent.SubmitKey)
        runCurrent()

        assertTrue(viewModel.uiState.value.checking)
        assertFalse(viewModel.uiState.value.canSubmit)
        answer.complete(DomainResult.Success(Unit))
    }

    @Test
    fun `키를 고치면 앞의 오류 문구가 사라진다`() = runTest(dispatcher) {
        val viewModel = viewModel(provider = KeyProvider.Gemini)
        viewModel.onIntent(KeyEntryIntent.ChangeKey("bad-key"))
        viewModel.onIntent(KeyEntryIntent.SubmitKey)
        advanceUntilIdle()

        viewModel.onIntent(KeyEntryIntent.ChangeKey("bad-key2"))

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `화면이 이동을 처리했다고 알리면 이동 필드를 지운다`() = runTest(dispatcher) {
        val viewModel = viewModel(provider = KeyProvider.Gemini)
        viewModel.onIntent(KeyEntryIntent.ChangeKey("good-key"))
        viewModel.onIntent(KeyEntryIntent.SubmitKey)
        advanceUntilIdle()

        viewModel.onIntent(KeyEntryIntent.NavigationHandled)

        assertNull(viewModel.uiState.value.navigation)
    }
}

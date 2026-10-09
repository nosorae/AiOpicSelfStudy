package io.github.nosorae.aiopic.ui.featuresettings

import io.github.nosorae.aiopic.ui.featuresettings.practice.QuestionImportResult
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 자기 문항 파일 불러오기(F6.3). 불러온 문항이 연습 풀에 들어가는 쪽은 앱 층 `AppSettingsTest` 가 본다. */
class QuestionImportTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeSettingsStore()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val twoQuestions = """
        [{"id": "my-1", "text": "Tell me about your home.", "type": "description", "topic": "home"},
         {"id": "my-2", "text": "What do you do on weekends?", "type": "routine", "topic": "home"}]
    """

    private fun newViewModel() = settingsViewModel(store, FakeKeyVault())

    @Test
    fun `F6_3 맞는 파일이면 저장하고 불러온 문항 수를 보인다`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ImportQuestions(twoQuestions))
        advanceUntilIdle()

        assertEquals(twoQuestions, store.questionFile)
        assertEquals(2, viewModel.uiState.value.importedQuestionCount)
        assertEquals(QuestionImportResult.Imported(2), viewModel.uiState.value.questionImport)
    }

    @Test
    fun `F6_3 틀린 파일이면 저장하지 않고 이유를 보인다`() = runTest(dispatcher) {
        store.questionFile = twoQuestions
        val viewModel = newViewModel()
        advanceUntilIdle()

        val duplicated = """[{"id": "x", "text": "a", "type": "t", "topic": "p"}, {"id": "x", "text": "b", "type": "t", "topic": "p"}]"""
        viewModel.onIntent(SettingsIntent.ImportQuestions(duplicated))
        advanceUntilIdle()

        assertEquals(twoQuestions, store.questionFile)
        assertEquals(2, viewModel.uiState.value.importedQuestionCount)
        val rejected = assertIs<QuestionImportResult.Invalid>(viewModel.uiState.value.questionImport)
        assertTrue("x" in rejected.reason, rejected.reason)
    }

    @Test
    fun `F6_3 JSON 이 아니거나 빈 목록이면 이유를 보인다`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        listOf("not json", "[]", """[{"id": "x"}]""").forEach { file ->
            viewModel.onIntent(SettingsIntent.ImportQuestions(file))
            advanceUntilIdle()
            assertIs<QuestionImportResult.Invalid>(viewModel.uiState.value.questionImport, file)
        }
        assertNull(store.questionFile)
        assertEquals(0, viewModel.uiState.value.importedQuestionCount)
    }

    @Test
    fun `파일을 읽지 못했으면 읽지 못했다고 보인다`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ImportQuestions(null))
        advanceUntilIdle()

        assertEquals(QuestionImportResult.Unreadable, viewModel.uiState.value.questionImport)
        assertNull(store.questionFile)
    }

    @Test
    fun `이미 불러온 파일이 있으면 설정을 열 때 문항 수가 보이고 안내는 없다`() = runTest(dispatcher) {
        store.questionFile = twoQuestions

        val viewModel = newViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.importedQuestionCount)
        assertNull(viewModel.uiState.value.questionImport)
    }
}

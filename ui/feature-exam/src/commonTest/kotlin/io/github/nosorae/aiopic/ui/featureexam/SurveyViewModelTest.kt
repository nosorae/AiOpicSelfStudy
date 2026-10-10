package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.question.model.SurveyAnswers
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

/** 설정에서 여는 서베이. 저장한 답을 보이고 바꾼 답을 저장한다. */
class SurveyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeSurveyStore(SurveyAnswers(mapOf("work" to "student", "home" to "alone"), listOf("영화", "요리")))

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SurveyViewModel(LoadSurveyAnswersUseCase(store, testContent), SaveSurveyAnswersUseCase(store))

    @Test
    fun `설정의 서베이는 저장한 답을 보이고 같은 흐름으로 바꾼 답을 저장하면 돌아간다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(setOf("영화", "요리"), viewModel.uiState.value.survey?.topics)

        viewModel.onIntent(SurveyIntent.ChooseOption("office"))
        viewModel.onIntent(SurveyIntent.ChooseOption("long"))
        viewModel.onIntent(SurveyIntent.ChooseOption("family"))
        viewModel.onIntent(SurveyIntent.ToggleTopic("공원"))
        viewModel.onIntent(SurveyIntent.Next)
        viewModel.onIntent(SurveyIntent.Submit)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.done)
        assertEquals(SurveyAnswers(mapOf("work" to "office", "period" to "long", "home" to "family"), listOf("영화", "공원", "요리")), store.saved)
    }

    @Test
    fun `F2_2 묶음 최소를 못 채우면 저장하지 않는다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(SurveyIntent.ToggleTopic("요리"))
        viewModel.onIntent(SurveyIntent.Submit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.done)
        assertEquals(listOf("영화", "요리"), store.saved?.topics)
    }

    @Test
    fun `답을 바꾸기 전에는 바꾼 답이 없고 바꿨다가 되돌리면 다시 없다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.edited)

        viewModel.onIntent(SurveyIntent.ToggleTopic("공원"))
        assertTrue(viewModel.uiState.value.edited)

        viewModel.onIntent(SurveyIntent.ToggleTopic("공원"))
        assertFalse(viewModel.uiState.value.edited)
    }
}

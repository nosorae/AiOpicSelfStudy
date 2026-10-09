package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
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
import kotlin.test.assertIs
import kotlin.test.assertNull

class AnswerReportViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `F7_1 한 문항 연습 답변도 같은 화면에서 보인다`() = runTest(dispatcher) {
        val store = FakeReportStore(answers = answers(examSessionId = null, count = 2))

        val viewModel = AnswerReportViewModel("a1", ObserveAnswerUseCase(store))
        advanceUntilIdle()

        assertEquals("a1", viewModel.uiState.value.answer?.id)
    }

    @Test
    fun `분석이 끝나면 대기 화면이 피드백으로 바뀐다`() = runTest(dispatcher) {
        val store = FakeReportStore(answers = answers("s1", 1).map { it.copy(analysis = AnswerAnalysis.Pending) })
        val viewModel = AnswerReportViewModel("a0", ObserveAnswerUseCase(store))
        advanceUntilIdle()

        store.answers.value = answers("s1", 1)
        advanceUntilIdle()

        assertIs<AnswerAnalysis.Done>(viewModel.uiState.value.answer?.analysis)
    }

    @Test
    fun `없는 답변이면 불러오기를 끝내고 비운다`() = runTest(dispatcher) {
        val viewModel = AnswerReportViewModel("none", ObserveAnswerUseCase(FakeReportStore()))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loading)
        assertNull(viewModel.uiState.value.answer)
    }
}

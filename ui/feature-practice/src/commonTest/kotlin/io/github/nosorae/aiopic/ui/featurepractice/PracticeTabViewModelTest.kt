package io.github.nosorae.aiopic.ui.featurepractice

import io.github.nosorae.aiopic.engine.question.model.QuestionType
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

class PracticeTabViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repository: FakePracticeRepository) =
        PracticeTabViewModel(PickPracticeQuestionUseCase(repository, typeNames = mapOf(QuestionType("roleplay") to "롤플레이")))

    @Test
    fun `F4_1 연습 탭은 다음 문항을 낼 약한 유형과 점수를 보인다`() = runTest(dispatcher) {
        val roleplay = question("p1", "roleplay")
        val routine = question("r1", "routine")
        val vm = viewModel(FakePracticeRepository(pool = listOf(routine, roleplay), answers = mutableListOf(scored("a1", routine, 80), scored("a2", roleplay, 40))))
        assertFalse(vm.uiState.value.loaded)

        advanceUntilIdle()

        assertEquals(PracticeWeakness("롤플레이", 40), vm.uiState.value.weakness)
        assertTrue(vm.uiState.value.loaded)
    }

    @Test
    fun `분석한 답변이 없으면 약한 유형 없이 모든 유형에서 고른다고 보인다`() = runTest(dispatcher) {
        val vm = viewModel(FakePracticeRepository())

        advanceUntilIdle()

        assertNull(vm.uiState.value.weakness)
        assertTrue(vm.uiState.value.loaded)
    }
}

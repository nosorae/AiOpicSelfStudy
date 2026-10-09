package io.github.nosorae.aiopic.ui.featuresettings.export

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlagExportViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private class FakeStore(answers: List<Answer>, flags: Set<CorrectionFlag>) : FlagExportStore {
        val answers = MutableStateFlow(answers)
        val flags = MutableStateFlow(flags)

        override fun answers(): Flow<List<Answer>> = answers

        override fun flags(): Flow<Set<CorrectionFlag>> = flags
    }

    private fun answer(id: String, analysis: AnswerAnalysis) = Answer(id, Question("q", "text", QuestionType("t"), "topic"), "s1", emptyList(), analysis = analysis)

    private val done = AnswerAnalysis.Done(
        AnalyzedBy("gemini", "gemini-3.8-flash", "v2"),
        Feedback(
            transcript = "I go to the park. We play.",
            corrections = listOf(Correction("I go", "I went"), Correction("We play", "We played")),
            upgrades = emptyList(),
            tenseConsistent = false,
            sentenceCount = 2,
            wordsPerMinute = 90.0,
            pauses = emptyList(),
            roleplayQuestionCount = null,
            pronunciation = Pronunciation.NotEvaluated,
            score = 50,
        ),
    )

    @Test
    fun `F8_3 표시한 교정을 답변 id 와 교정 순서로 가리키고 전문과 교정 내용과 분석 모델을 함께 JSON 으로 낸다`() = runTest(dispatcher) {
        val store = FakeStore(listOf(answer("a1", done)), setOf(CorrectionFlag("a1", 1)))

        val viewModel = FlagExportViewModel(ObserveFlagExportUseCase(store))
        advanceUntilIdle()

        val expected = """
            [{"answerId": "a1", "index": 1, "transcript": "I go to the park. We play.", "original": "We play", "corrected": "We played",
              "provider": "gemini", "model": "gemini-3.8-flash", "promptVersion": "v2"}]
        """
        assertEquals(1, viewModel.uiState.value.count)
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(viewModel.uiState.value.json))
    }

    @Test
    fun `답변 저장 순서와 교정 순서로 줄 세우고 가리키는 교정이 없는 표시는 뺀다`() = runTest(dispatcher) {
        val flags = setOf(CorrectionFlag("a2", 0), CorrectionFlag("a1", 1), CorrectionFlag("a1", 0), CorrectionFlag("a1", 5), CorrectionFlag("a3", 0), CorrectionFlag("gone", 0))
        val store = FakeStore(listOf(answer("a1", done), answer("a2", done), answer("a3", AnswerAnalysis.Pending)), flags)

        val viewModel = FlagExportViewModel(ObserveFlagExportUseCase(store))
        advanceUntilIdle()

        val rows = Json.parseToJsonElement(viewModel.uiState.value.json).toString()
        assertEquals(3, viewModel.uiState.value.count)
        assertTrue(rows.indexOf("\"a1\",\"index\":0") < rows.indexOf("\"a1\",\"index\":1"), rows)
        assertTrue(rows.indexOf("\"a1\",\"index\":1") < rows.indexOf("\"a2\""), rows)
    }

    @Test
    fun `표시가 없으면 0개와 빈 배열이다`() = runTest(dispatcher) {
        val viewModel = FlagExportViewModel(ObserveFlagExportUseCase(FakeStore(listOf(answer("a1", done)), emptySet())))
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.count)
        assertEquals("[]", Json.parseToJsonElement(viewModel.uiState.value.json).toString())
    }

    @Test
    fun `복사했다고 알리면 안내 상태를 켜고 내보낼 내용은 그대로 둔다`() = runTest(dispatcher) {
        val viewModel = FlagExportViewModel(ObserveFlagExportUseCase(FakeStore(listOf(answer("a1", done)), setOf(CorrectionFlag("a1", 0)))))
        advanceUntilIdle()

        viewModel.onIntent(FlagExportIntent.JsonCopied)

        assertTrue(viewModel.uiState.value.copied)
        assertEquals(1, viewModel.uiState.value.count)
    }
}

package io.github.nosorae.aiopic.engine.answer.weakness

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import kotlin.test.Test
import kotlin.test.assertEquals

class WeaknessTest {
    private val by = AnalyzedBy(provider = "gemini", modelId = "test-model", promptVersion = "answer-v1")
    private val description = QuestionType("description")
    private val roleplay = QuestionType("roleplay")

    /** [deducted] 는 교정마다 깎은 점수다. null 은 깎은 점수를 받기 전의 옛 답변이다. */
    private fun answer(id: String, type: QuestionType, score: Int, corrections: Int = 0, deducted: List<Int?> = List(corrections) { null }) = Answer(
        id = id,
        question = Question(id = "q-$id", text = "Tell me.", type = type, topic = "home"),
        examSessionId = "s1",
        chunkPaths = emptyList(),
        analysis = AnswerAnalysis.Done(
            by,
            Feedback(
                transcript = "I go to park.",
                corrections = deducted.map { Correction(original = "go to park", corrected = "I go to the park.", deducted = it) },
                upgrades = emptyList(),
                tenseConsistent = true,
                sentenceCount = 1,
                wordsPerMinute = 100.0,
                pauses = emptyList(),
                roleplayQuestionCount = null,
                pronunciation = Pronunciation.NotEvaluated,
                score = score,
            ),
        ),
    )

    @Test
    fun `F7_3 문항 유형별 평균 점수를 낮은 유형부터 낸다`() {
        val answers = listOf(
            answer("a1", description, 80),
            answer("a2", roleplay, 40),
            answer("a3", description, 61),
            answer("a4", roleplay, 50),
        )

        assertEquals(
            listOf(TypeWeakness(roleplay, score = 45), TypeWeakness(description, score = 71)),
            weaknessScores(answers),
        )
    }

    @Test
    fun `분석이 안 끝났거나 평가할 수 없는 답변은 세지 않는다`() {
        val answers = listOf(
            answer("a1", description, 70),
            answer("a2", description, 0).copy(analysis = AnswerAnalysis.Pending),
            answer("a3", description, 0).copy(analysis = AnswerAnalysis.Unscorable(by, UnscorableReason.Silent)),
        )

        assertEquals(listOf(TypeWeakness(description, score = 70)), weaknessScores(answers))
    }

    @Test
    fun `F8_2 깎은 점수가 없는 옛 답변은 틀림 표시가 붙으면 평균에서 뺀다`() {
        val answers = listOf(
            answer("a1", description, 40, corrections = 3),
            answer("a2", description, 80, corrections = 1),
            answer("a3", roleplay, 30, corrections = 1),
        )
        val flagged = setOf(CorrectionFlag("a1", 0), CorrectionFlag("a1", 2), CorrectionFlag("a3", 0))

        assertEquals(listOf(TypeWeakness(description, score = 80)), weaknessScores(answers, flagged))
    }

    @Test
    fun `없는 교정이나 다른 답변을 가리키는 표시는 무시한다`() {
        val answers = listOf(answer("a1", description, 45, corrections = 1))
        val flagged = setOf(CorrectionFlag("a1", 5), CorrectionFlag("other", 0))

        assertEquals(listOf(TypeWeakness(description, score = 45)), weaknessScores(answers, flagged))
    }

    @Test
    fun `F8_2 교정 둘 중 하나만 틀림 표시하면 그 교정이 깎은 점수만 되돌린다`() {
        val answers = listOf(answer("a1", description, 60, deducted = listOf(15, 10)))

        assertEquals(listOf(TypeWeakness(description, score = 75)), weaknessScores(answers, setOf(CorrectionFlag("a1", 0))))
    }

    @Test
    fun `F8_2 교정 둘 다 틀림 표시하면 둘 다 되돌린다`() {
        val answers = listOf(
            answer("a1", description, 60, deducted = listOf(15, 10)),
            answer("a2", description, 70),
        )
        val flagged = setOf(CorrectionFlag("a1", 0), CorrectionFlag("a1", 1))

        assertEquals(listOf(TypeWeakness(description, score = 78)), weaknessScores(answers, flagged))
    }

    @Test
    fun `F8_2 표시한 교정에 깎은 점수가 없으면 그 답변은 빼고 표시 안 한 교정의 빈 값은 상관없다`() {
        val answers = listOf(
            answer("a1", description, 60, deducted = listOf(15, null)),
            answer("a2", description, 50, deducted = listOf(null, 20)),
            answer("a3", description, 90),
        )
        val flagged = setOf(CorrectionFlag("a1", 1), CorrectionFlag("a2", 1))

        assertEquals(listOf(TypeWeakness(description, score = 80)), weaknessScores(answers, flagged))
    }
}

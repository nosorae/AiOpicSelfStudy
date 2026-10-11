package io.github.nosorae.aiopic.engine.answer.check

import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.Pause
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.model.Upgrade
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.session.model.GradeCheck
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class OutputCheckTest {
    private val feedback = Feedback(
        transcript = "I live in a apartment.\nLast year I move to Seoul.",
        corrections = listOf(Correction("a apartment", "I live in an apartment."), Correction("I lives", "I live.")),
        upgrades = listOf(Upgrade("apartment. Last year", "Last year I moved."), Upgrade("never said", "x")),
        tenseConsistent = false,
        sentenceCount = 2,
        wordsPerMinute = 110.0,
        pauses = listOf(Pause(1.seconds, 2.seconds), Pause((-1).seconds, 1.seconds)),
        roleplayQuestionCount = null,
        pronunciation = Pronunciation.Evaluated("Clear."),
        score = 60,
    )
    private val clean = feedback.copy(
        corrections = feedback.corrections.take(1),
        upgrades = feedback.upgrades.take(1),
        pauses = feedback.pauses.take(1),
    )
    private val scale = GradeScale(listOf("NL", "IM", "IH", "AL"))
    private val grade = SessionGrade(checks = listOf(GradeCheck("C1", true)), low = "IM", high = "IH", reasons = listOf("r"), brokeAt = listOf(2))

    @Test
    fun `F5_4 전문에 없는 교정 구절을 버린다`() {
        assertEquals(clean.corrections, checkFeedback(feedback).value?.corrections)
    }

    @Test
    fun `F5_4 전문에 없는 한 등급 위 표현을 버린다`() {
        assertEquals(clean.upgrades, checkFeedback(feedback).value?.upgrades)
    }

    @Test
    fun `F5_4 줄바꿈이 낀 구절도 띄어쓰기 하나로 보면 전문에 있다`() {
        assertEquals(1, checkFeedback(feedback).value?.upgrades?.size)
    }

    @Test
    fun `F5_4 음수 시각의 멈춤을 버린다`() {
        assertEquals(clean.pauses, checkFeedback(feedback).value?.pauses)
    }

    @Test
    fun `F5_4 버린 항목이 있으면 문제로 알린다`() {
        assertEquals(3, checkFeedback(feedback).problems.size)
    }

    @Test
    fun `F5_4 모두 통과하면 문제가 없고 값이 그대로다`() {
        assertEquals(Checked(clean, emptyList()), checkFeedback(clean))
    }

    @Test
    fun `F8_2 교정이 깎은 점수가 0 이상이고 합이 100 - score 이하면 그대로 둔다`() {
        val scored = clean.copy(corrections = listOf(Correction("a apartment", "I live in an apartment.", deducted = 40)))

        assertEquals(Checked(scored, emptyList()), checkFeedback(scored))
    }

    @Test
    fun `F8_2 깎은 점수가 음수거나 합이 100 - score 를 넘으면 깎은 점수만 버리고 교정은 남긴다`() {
        val corrections = listOf(Correction("a apartment", "x", deducted = 30), Correction("Last year", "y", deducted = 11))
        val negative = clean.copy(corrections = listOf(Correction("a apartment", "x", deducted = -5)))

        listOf(clean.copy(corrections = corrections), negative).forEach { bad ->
            val checked = checkFeedback(bad)
            assertEquals(bad.corrections.map { it.copy(deducted = null) }, checked.value?.corrections)
            assertEquals(1, checked.problems.size)
        }
    }

    @Test
    fun `F5_4 점수가 0 에서 100 사이를 벗어나면 버릴 수 없어서 값이 없다`() {
        assertNull(checkFeedback(clean.copy(score = 101)).value)
    }

    @Test
    fun `F5_4 문장 수나 말 속도나 질문 수가 음수면 값이 없다`() {
        val bad = listOf(clean.copy(sentenceCount = -1), clean.copy(wordsPerMinute = -1.0), clean.copy(wordsPerMinute = Double.NaN), clean.copy(roleplayQuestionCount = -1))

        assertTrue(bad.all { checkFeedback(it).value == null })
    }

    @Test
    fun `F5_3 회차 등급이 등급 체계 안이고 낮은 쪽이 높은 쪽보다 높지 않으면 통과한다`() {
        assertEquals(Checked(grade, emptyList()), checkSessionGrade(grade, scale, answerCount = 3))
    }

    @Test
    fun `F5_4 등급 체계에 없는 등급이면 값이 없다`() {
        assertNull(checkSessionGrade(grade.copy(high = "S"), scale, answerCount = 3).value)
    }

    @Test
    fun `F5_4 낮은 등급이 높은 등급보다 높으면 값이 없다`() {
        assertNull(checkSessionGrade(grade.copy(low = "AL"), scale, answerCount = 3).value)
    }

    @Test
    fun `F5_4 문항 수를 벗어난 무너진 문항 번호를 버린다`() {
        val checked = checkSessionGrade(grade.copy(brokeAt = listOf(0, 2, 4)), scale, answerCount = 3)

        assertEquals(listOf(2) to 2, checked.value?.brokeAt to checked.problems.size)
    }

    @Test
    fun `F5_4 통과하면 다시 요청하지 않는다`() = runTest {
        var calls = 0

        val result = requestChecked(::checkFeedback) {
            calls++
            AnalysisResult.Success(clean)
        }

        assertEquals(AnalysisResult.Success(clean) to 1, result to calls)
    }

    @Test
    fun `F5_4 통과 못하면 한 번 다시 요청해 통과한 값을 쓴다`() = runTest {
        val responses = ArrayDeque(listOf(feedback, clean.copy(score = 70)))

        val result = requestChecked(::checkFeedback) { AnalysisResult.Success(responses.removeFirst()) }

        assertEquals(AnalysisResult.Success(clean.copy(score = 70)), result)
    }

    @Test
    fun `F5_4 다시 요청해도 통과 못하면 그 항목을 버린 값을 쓴다`() = runTest {
        var calls = 0

        val result = requestChecked(::checkFeedback) {
            calls++
            AnalysisResult.Success(feedback)
        }

        assertEquals(AnalysisResult.Success(clean) to 2, result to calls)
    }

    @Test
    fun `F5_4 다시 요청해도 버릴 수 없는 값이 틀리면 거절한다`() = runTest {
        val result = requestChecked(::checkFeedback) { AnalysisResult.Success(clean.copy(score = -5)) }

        assertEquals(AnalysisResult.Failed(AnalysisError.Rejected), result)
    }

    @Test
    fun `F5_4 다시 요청이 실패하면 첫 값에서 항목을 버려 쓴다`() = runTest {
        val responses = ArrayDeque<AnalysisResult<Feedback>>(listOf(AnalysisResult.Success(feedback), AnalysisResult.Failed(AnalysisError.Retryable)))

        assertEquals(AnalysisResult.Success(clean), requestChecked(::checkFeedback) { responses.removeFirst() })
    }

    @Test
    fun `첫 요청이 실패하면 검사 없이 그 실패를 돌려준다`() = runTest {
        val failed = AnalysisResult.Failed(AnalysisError.InvalidKey)

        assertEquals(failed, requestChecked(::checkFeedback) { failed })
    }
}

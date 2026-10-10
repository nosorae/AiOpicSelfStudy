package io.github.nosorae.aiopic.packs.opicen

import io.github.nosorae.aiopic.engine.session.model.ExamStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class OpicEnTest {
    private val steps = OpicEn.examSpec.steps

    private fun askCount(level: Int) = steps.count { it is ExamStep.Ask && it.minLevel <= level }

    @Test
    fun `F2_1 서베이와 수준 선택과 점검 다음에 7문항과 재조정과 8문항 순서다`() {
        val shape = steps.map { if (it is ExamStep.Ask) "ask" else it::class.simpleName }

        assertEquals(listOf("Survey", "LevelSelect", "Check") + List(7) { "ask" } + "Recalibrate" + List(8) { "ask" }, shape)
    }

    @Test
    fun `F2_1 수준 1과 2 는 12문항이다`() {
        assertEquals(listOf(12, 12), (1..2).map(::askCount))
    }

    @Test
    fun `F2_1 수준 3부터 6 은 15문항이다`() {
        assertEquals(List(4) { 15 }, (3..6).map(::askCount))
    }

    @Test
    fun `F2_2 서베이는 12개 미만이면 다음으로 못 간다`() {
        assertEquals(ExamStep.Survey(minTopics = 12), steps.first())
    }

    @Test
    fun `F2_2 서베이 Part 질문은 주제 앞에 셋이고 보기가 비어 있지 않다`() {
        assertEquals(3, OpicEn.surveyParts.size)
        assertTrue(OpicEn.surveyParts.all { (question, options) -> question.isNotBlank() && options.isNotEmpty() })
    }

    @Test
    fun `시험 시간은 40분이고 수준은 6개다`() {
        assertEquals(40.minutes to 6, OpicEn.examSpec.timeLimit to OpicEn.examSpec.levelCount)
    }

    @Test
    fun `1번은 자기소개이고 11번부터 13번은 롤플레이다`() {
        val asks = steps.filterIsInstance<ExamStep.Ask>().map { it.questionType }

        assertEquals(OpicQuestionType.SelfIntro.type, asks[0])
        assertEquals(listOf(OpicQuestionType.RoleplayAsk, OpicQuestionType.RoleplaySolve, OpicQuestionType.RoleplayExperience).map { it.type }, asks.subList(10, 13))
    }

    @Test
    fun `F5_3 등급은 NL 부터 AL 까지 9개다`() {
        assertEquals(listOf("NL", "NM", "NH", "IL", "IM1", "IM2", "IM3", "IH", "AL"), OpicEn.gradeScale.grades)
    }
}

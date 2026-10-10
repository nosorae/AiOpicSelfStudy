package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.question.model.SurveyAnswers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurveyStateTest {
    private val start = SurveyState(testSurvey)

    @Test
    fun `F2_2 하나만 고르는 질문은 고르는 즉시 다음 화면으로 가고 하위 질문이 바로 다음에 나온다`() {
        val afterWork = start.choose("office")

        assertEquals(1, afterWork.screen)
        assertEquals("period", afterWork.question?.id)
        assertEquals(5, afterWork.screenCount, "일, 근무 기간, 사는 곳, 여가, 취미")
        assertEquals("home", start.choose("student").question?.id)
    }

    @Test
    fun `F2_2 묶음 화면은 질문 다음에 하나씩 나오고 묶음 최소를 채워야 다음으로 간다`() {
        val leisure = start.choose("student").choose("alone")
        assertNull(leisure.question)
        assertEquals("leisure", leisure.group?.id)
        assertFalse(leisure.canNext)
        assertEquals(leisure, leisure.next())

        val hobbies = leisure.toggle("영화").next()
        assertEquals("hobbies", hobbies.group?.id)
        assertTrue(hobbies.isLast)
    }

    @Test
    fun `F2_2 마지막 화면은 묶음 최소와 전체 최소를 모두 채워야 낸다`() {
        val last = start.choose("student").choose("alone").toggle("영화").next()
        assertFalse(last.canNext)
        assertTrue(last.toggle("요리").canNext)
        assertTrue(last.toggle("요리").canSubmit)
    }

    @Test
    fun `뒤로 가면 앞 화면이고 다른 보기를 고르면 안 보이게 된 하위 질문의 답은 저장하지 않는다`() {
        val state = start.choose("office").choose("long").back().back().choose("student").choose("family").toggle("영화").toggle("요리")

        assertEquals(SurveyAnswers(mapOf("work" to "student", "home" to "family"), listOf("영화", "요리")), state.answers())
        assertEquals(0, start.back().screen)
    }

    @Test
    fun `F2_2 저장한 답으로 미리 채우고 서베이에 없는 주제는 뺀다`() {
        val state = SurveyState.prefilled(testSurvey, SurveyAnswers(mapOf("work" to "student"), listOf("요리", "없어진 주제")))

        assertEquals(mapOf("work" to "student"), state.choices)
        assertEquals(setOf("요리"), state.topics)
        assertEquals(0, state.screen)
    }
}

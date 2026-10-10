package io.github.nosorae.aiopic.engine.question.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SurveyTest {
    private val period = SurveyQuestion("period", "얼마나", listOf(SurveyOption("short", "짧게"), SurveyOption("long", "오래")))
    private val job = SurveyQuestion(
        "job",
        "하는 일",
        listOf(SurveyOption("office", "회사", next = listOf(period)), SurveyOption("none", "없음")),
    )
    private val home = SurveyQuestion("home", "사는 곳", listOf(SurveyOption("alone", "혼자")))

    private val survey = Survey(
        questions = listOf(job, home),
        groups = listOf(
            TopicGroup("leisure", "여가", min = 2, topics = listOf(Topic("movies", "영화"), Topic("parks", "공원"), Topic("beach", "해변"))),
            TopicGroup("sports", "운동", min = 1, topics = listOf(Topic("jogging", "조깅"), Topic("golf", "골프"))),
        ),
        minTopics = 3,
    )

    @Test
    fun `F2_2 고른 보기의 하위 질문이 바로 다음에 나온다`() {
        assertEquals(listOf("job", "home"), survey.visibleQuestions(emptyMap()).map { it.id })
        assertEquals(listOf("job", "period", "home"), survey.visibleQuestions(mapOf("job" to "office")).map { it.id })
        assertEquals(listOf("job", "home"), survey.visibleQuestions(mapOf("job" to "none", "period" to "long")).map { it.id })
    }

    @Test
    fun `F2_2 묶음마다 최소 개수와 전체 최소 개수를 모두 채워야 받는다`() {
        assertTrue(survey.accepts(listOf("movies", "parks", "jogging")))
        // 전체는 3개지만 운동이 0개다
        assertFalse(survey.accepts(listOf("movies", "parks", "beach")))
        // 묶음은 채웠지만 전체가 4개보다 적다
        assertFalse(survey.copy(minTopics = 4).accepts(listOf("movies", "parks", "jogging")))
    }

    @Test
    fun `가까운 주제는 고른 주제와 같은 묶음의 나머지다`() {
        assertEquals(listOf("parks", "beach"), survey.nearTopics(listOf("movies")))
        assertEquals(listOf("parks", "beach", "golf"), survey.nearTopics(listOf("movies", "jogging")))
    }
}

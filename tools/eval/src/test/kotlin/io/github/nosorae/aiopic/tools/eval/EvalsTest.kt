package io.github.nosorae.aiopic.tools.eval

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EvalsTest {
    private fun grade(low: String, high: String, vararg met: Boolean) = GradeResult(met.mapIndexed { i, m -> Check("C${i + 1}", m) }, low, high)

    @Test
    fun sameInputCountsTheMostCommonRangeAndSplitChecks() {
        val runs = List(4) { grade("IM1", "IM2", true, true, false) } + grade("IM2", "IM3", true, false, false)

        val table = consistencyTable(runs)

        assertContains(table, "| 5 | IM2~IM3 |")
        assertContains(table, "같은 범위: 4/5 (IM1~IM2)")
        assertContains(table, "답이 갈린 체크: C2")
    }

    @Test
    fun wrongAnswerCountsCorrectionsThatQuoteTheInjectedPhrase() {
        val errors = listOf(InjectedError("I go to Busan", "went"), InjectedError("a amazing", "an amazing"), InjectedError("I eat", "ate"))
        val corrections = listOf(
            // 받아쓰기가 대소문자와 문장부호를 바꿔도 잡은 것으로 센다.
            Correction("last summer, i go to busan.", "Last summer, I went to Busan."),
            Correction("It was a amazing trip", "It was an amazing trip"),
        )

        val table = caughtTable(errors, corrections)

        assertContains(table, "| I go to Busan | went | 예 |")
        assertContains(table, "| I eat | ate | 아니오 |")
        assertContains(table, "잡은 비율: 2/3")
    }

    @Test
    fun levelsAreInOrderWhenRangeMiddlesRise() {
        val inOrder = levelTable(listOf("NL" to grade("NL", "NM"), "IM2" to grade("IM1", "IM2"), "AL" to grade("IH", "AL")))
        assertContains(inOrder, "| IM2 | IM1~IM2 |")
        assertContains(inOrder, "등급 순서: 맞다")

        val swapped = levelTable(listOf("NL" to grade("IM1", "IM2"), "IM2" to grade("NL", "NM"), "AL" to grade("IH", "AL")))
        assertContains(swapped, "등급 순서: 틀리다")
    }

    @Test
    fun gradeOutsideTheScaleBreaksTheOrder() {
        val table = levelTable(listOf("NL" to grade("NL", "NL"), "AL" to grade("A1", "B2")))
        assertContains(table, "등급 순서: 틀리다")
    }

    @Test
    fun answerSchemaAsksDeductedScorePerCorrection() {
        val correction = Json.parseToJsonElement(ANSWER_SCHEMA).jsonObject["properties"]!!.jsonObject["corrections"]!!.jsonObject["items"]!!.jsonObject

        assertEquals("""["original","corrected","deducted"]""", correction["required"].toString())
        assertEquals("""{"type":"integer"}""", correction["properties"]!!.jsonObject["deducted"].toString())
    }

    // F8.4 프롬프트를 고치기 전후로 만드는 품질 확인 보고서다. 프롬프트 버전과 모델이 보고서에 남아야 버전끼리 비교한다.
    @Test
    fun runPutsThreeTablesInOneReportWithGivenModelReplies() {
        val asked = mutableListOf<String>()
        val eval = Eval(
            speak = { it.encodeToByteArray() },
            ask = { prompt, _, audio ->
                asked += prompt
                assertTrue(audio.isNotEmpty())
                when {
                    prompt.startsWith("answer") -> """{"transcript":"I go to Busan","corrections":[{"original":"I go to Busan","corrected":"I went to Busan"}]}"""
                    "NL script" in prompt -> """{"checks":[{"id":"C1","met":false}],"low":"NL","high":"NM","reasons":[],"brokeAt":[]}"""
                    "AL script" in prompt -> """{"checks":[{"id":"C1","met":true}],"low":"IH","high":"AL","reasons":[],"brokeAt":[]}"""
                    else -> """{"checks":[{"id":"C1","met":true}],"low":"IM1","high":"IM2","reasons":[],"brokeAt":[]}"""
                }
            },
        )
        val input = EvalInput(
            wrong = WrongAnswer(Answer("Tell me about a trip.", "past-experience", "I go to Busan"), listOf(InjectedError("I go to Busan", "went"))),
            levels = listOf(
                Level("NL", listOf(Answer("Q1", "description", "NL script"))),
                Level("IM2", listOf(Answer("Q1", "description", "IM script"), Answer("Q2", "roleplay-ask", "IM script 2"))),
                Level("AL", listOf(Answer("Q1", "description", "AL script"))),
            ),
            repeatLevel = "IM2",
        )
        val answerPrompt = PromptFile.parse("promptVersion: a-v1\nanswer {{question}} {{questionType}} {{resumedAt}}")
        val gradePrompt = PromptFile.parse("promptVersion: g-v1\ngrade {{answers}}")

        val report = eval.report(input, answerPrompt, gradePrompt, model = "test-model")

        // 같은 입력 5번에서 받은 첫 결과를 수준 셋 비교에 다시 쓴다. 답변 분석 1번, 회차 등급 5 + 2번이다.
        assertEquals(8, asked.size)
        assertEquals(1, asked.count { it.startsWith("answer") })
        assertFalse(asked.any { "{{" in it })
        assertContains(report, "a-v1")
        assertContains(report, "g-v1")
        assertContains(report, "test-model")
        assertContains(report, "같은 범위: 5/5 (IM1~IM2)")
        assertContains(report, "잡은 비율: 1/1")
        assertContains(report, "등급 순서: 맞다")
        // 표의 구분 줄과 각 줄이 `|` 로 시작해야 마크다운 표로 보인다.
        assertContains(report, "\n|---|---|\n| 1 | IM1~IM2 |")
    }
}

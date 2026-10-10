package io.github.nosorae.aiopic.tools.eval

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put

/**
 * 정답 등급 없이 프롬프트 품질을 보는 세 가지(product.md 6절). 같은 입력 5번, 일부러 틀린 답, 수준 다른 답 셋을 돌려
 * 프롬프트 버전마다 마크다운 표 하나로 남긴다.
 * [speak] 는 대본을 녹음으로 바꾸고 [ask] 는 프롬프트, 응답 스키마, 녹음을 모델에 보내 받은 JSON 글을 돌려준다.
 */
class Eval(
    private val speak: (String) -> ByteArray,
    private val ask: (prompt: String, schema: String, audio: List<ByteArray>) -> String,
) {
    fun report(input: EvalInput, answerPrompt: PromptFile, gradePrompt: PromptFile, model: String): String {
        val repeat = input.levels.single { it.name == input.repeatLevel }
        val runs = List(REPEAT) { grade(repeat, gradePrompt) }
        val levels = input.levels.map { it.name to if (it === repeat) runs.first() else grade(it, gradePrompt) }
        val wrong = analyze(input.wrong.answer, answerPrompt)
        // 표 줄이 `|` 로 시작해서 trimMargin 을 쓰면 표의 `|` 까지 지워진다. 줄을 목록으로 잇는다.
        return listOf(
            "# 품질 확인",
            "",
            "- 모델: $model",
            "- 답변 프롬프트: ${answerPrompt.version}",
            "- 회차 등급 프롬프트: ${gradePrompt.version}",
            "",
            "## 같은 입력 ${REPEAT}번 (${repeat.name} 수준 회차)",
            "",
            consistencyTable(runs),
            "",
            "## 일부러 틀린 답",
            "",
            caughtTable(input.wrong.errors, wrong.corrections),
            "",
            "## 수준 다른 답 셋",
            "",
            levelTable(levels),
            "",
        ).joinToString("\n")
    }

    private fun analyze(answer: Answer, prompt: PromptFile): AnswerResult {
        val text = prompt.fill(mapOf("question" to answer.question, "questionType" to answer.type, "resumedAt" to "none"))
        return json.decodeFromString(ask(text, ANSWER_SCHEMA, listOf(speak(answer.script))))
    }

    // ponytail: 회차 등급에 답변 피드백 대신 문항과 대본만 넣는다. 답변 분석을 문항마다 부르면 무료 한도를 금방 넘어서다.
    // 앱과 같은 입력이 필요해지면 analyze 결과를 그대로 넣는다.
    private fun grade(level: Level, prompt: PromptFile): GradeResult {
        val answers = buildJsonArray {
            level.answers.forEachIndexed { i, answer ->
                addJsonObject {
                    put("number", i + 1)
                    put("question", answer.question)
                    put("questionType", answer.type)
                    put("transcript", answer.script)
                }
            }
        }
        val text = prompt.fill(mapOf("answers" to answers.toString()))
        return json.decodeFromString(ask(text, GRADE_SCHEMA, level.answers.map { speak(it.script) }))
    }

    private companion object {
        const val REPEAT = 5
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** 같은 회차를 여러 번 등급 매긴 결과에서 같은 범위가 몇 번 나왔는지와 답이 갈린 체크 문장을 본다. */
fun consistencyTable(runs: List<GradeResult>): String {
    val ranges = runs.map { it.range }
    val (common, count) = ranges.groupingBy { it }.eachCount().maxBy { it.value }
    val split = runs.flatMap { it.checks }.groupBy { it.id }.filterValues { checks -> checks.map { it.met }.distinct().size > 1 }.keys
    val rows = ranges.mapIndexed { i, range -> "| ${i + 1} | $range |" }
    return (listOf("| 회 | 등급 범위 |", "|---|---|") + rows).joinToString("\n") +
        "\n\n같은 범위: $count/${runs.size} ($common)\n답이 갈린 체크: ${split.joinToString().ifEmpty { "없음" }}"
}

/** 대본에 넣은 오류마다 교정의 original 이 그 구절을 담고 있으면 잡은 것으로 센다. 받아쓰기의 대소문자와 문장부호 차이는 무시한다. */
fun caughtTable(errors: List<InjectedError>, corrections: List<Correction>): String {
    val quoted = corrections.map { normalize(it.original) }
    val caught = errors.map { error -> quoted.any { normalize(error.wrong) in it } }
    val rows = errors.zip(caught) { error, hit -> "| ${error.wrong} | ${error.right} | ${if (hit) "예" else "아니오"} |" }
    return (listOf("| 넣은 오류 | 맞는 말 | 잡았나 |", "|---|---|---|") + rows).joinToString("\n") +
        "\n\n잡은 비율: ${caught.count { it }}/${errors.size}"
}

/** 대본을 쓴 수준 순서대로 받은 등급 범위의 가운데가 올라가면 맞다. 등급 체계 밖의 이름이 오면 틀리다. */
fun levelTable(levels: List<Pair<String, GradeResult>>): String {
    val middles = levels.map { (_, grade) -> listOf(grade.low, grade.high).map(GRADES::indexOf) }
    val inOrder = middles.none { -1 in it } && middles.map { it.sum() }.zipWithNext().all { (a, b) -> a < b }
    val rows = levels.map { (name, grade) -> "| $name | ${grade.range} |" }
    return (listOf("| 대본 수준 | 받은 등급 범위 |", "|---|---|") + rows).joinToString("\n") +
        "\n\n등급 순서: ${if (inOrder) "맞다" else "틀리다"}"
}

private fun normalize(text: String) = text.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

// ponytail: 시험 팩의 OpicGrade 와 같은 목록을 따로 둔다. 모듈 대상이 달라 의존하지 못해서다(build.gradle.kts).
val GRADES = listOf("NL", "NM", "NH", "IL", "IM1", "IM2", "IM3", "IH", "AL")

@Serializable
data class EvalInput(val wrong: WrongAnswer, val levels: List<Level>, val repeatLevel: String)

/** 대본 하나와 그 안에 일부러 넣은 오류들. */
@Serializable
data class WrongAnswer(val answer: Answer, val errors: List<InjectedError>)

/** [wrong] 은 대본에 그대로 있는 구절이다. */
@Serializable
data class InjectedError(val wrong: String, val right: String)

/** 한 수준으로 쓴 회차. 답변은 문항 순서다. */
@Serializable
data class Level(val name: String, val answers: List<Answer>)

/** [type] 은 시험 팩의 문항 유형 id 다. */
@Serializable
data class Answer(val question: String, val type: String, val script: String)

/** 답변 분석 응답에서 쓰는 부분. */
@Serializable
data class AnswerResult(val corrections: List<Correction>)

@Serializable
data class Correction(val original: String, val corrected: String)

/** 회차 등급 응답에서 쓰는 부분. */
@Serializable
data class GradeResult(val checks: List<Check>, val low: String, val high: String) {
    val range get() = "$low~$high"
}

@Serializable
data class Check(val id: String, val met: Boolean)

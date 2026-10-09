package io.github.nosorae.aiopic.engine.answer.check

import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.port.AnalysisError
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import kotlin.time.Duration

/**
 * 출력 검사(F5.4) 한 번의 결과. [value] 는 통과 못한 목록 항목을 버린 값이고, 점수처럼 버릴 수 없는 값이 틀렸으면 null 이다.
 * [problems] 가 있으면 [requestChecked] 가 한 번 다시 요청한다.
 */
data class Checked<out T>(val value: T?, val problems: List<String>)

/** 모델이 지어낸 교정을 사용자에게 보이지 않게 교정, 표현의 원문 구절이 전문에 그대로 있는지와 값 범위를 본다. */
fun checkFeedback(feedback: Feedback): Checked<Feedback> {
    val transcript = feedback.transcript.squashSpaces()
    val problems = mutableListOf<String>()
    fun <T> List<T>.keep(what: String, ok: (T) -> Boolean) = partition(ok).also { (_, dropped) -> dropped.forEach { problems += "$what: $it" } }.first

    val kept = feedback.copy(
        corrections = feedback.corrections.keep("전문에 없는 교정") { it.original.isNotBlank() && it.original.squashSpaces() in transcript },
        upgrades = feedback.upgrades.keep("전문에 없는 표현") { it.original.isNotBlank() && it.original.squashSpaces() in transcript },
        pauses = feedback.pauses.keep("범위 밖 멈춤") { it.start >= Duration.ZERO && it.length > Duration.ZERO },
    )
    val inRange = feedback.score in 0..100 &&
        feedback.sentenceCount >= 0 &&
        feedback.wordsPerMinute.let { it.isFinite() && it >= 0 } &&
        (feedback.roleplayQuestionCount ?: 0) >= 0
    if (!inRange) problems += "범위 밖 값: score=${feedback.score}, sentenceCount=${feedback.sentenceCount}, wpm=${feedback.wordsPerMinute}"
    return Checked(kept.takeIf { inRange }, problems)
}

/** 등급이 등급 체계 안에 있고 낮은 쪽이 높은 쪽보다 높지 않은지, 무너진 문항 번호가 1..[answerCount] 안인지 본다. */
fun checkSessionGrade(grade: SessionGrade, scale: GradeScale, answerCount: Int): Checked<SessionGrade> {
    val low = scale.grades.indexOf(grade.low)
    val high = scale.grades.indexOf(grade.high)
    val validRange = low >= 0 && high >= low
    val (broke, outside) = grade.brokeAt.partition { it in 1..answerCount }
    val problems = outside.map { "문항 수 밖 번호: $it" } + listOfNotNull("등급 범위가 틀렸다: ${grade.low} ~ ${grade.high}".takeUnless { validRange })
    return Checked(grade.copy(brokeAt = broke).takeIf { validRange }, problems)
}

/**
 * 요청하고 [check] 를 통과 못하면 한 번만 다시 요청한다(F5.4). 다시 받은 값도 통과 못하면 통과 못한 항목을 버린 값을 쓰고,
 * 버릴 수 없는 값이 틀렸으면 거절한다. 다시 요청이 실패하면 첫 값에서 항목을 버린 값을 쓴다.
 */
suspend fun <T : Any> requestChecked(check: (T) -> Checked<T>, request: suspend () -> AnalysisResult<T>): AnalysisResult<T> {
    val first = request()
    if (first !is AnalysisResult.Success) return first
    val firstChecked = check(first.value)
    if (firstChecked.problems.isEmpty()) return first
    val second = request()
    val fallback = if (second is AnalysisResult.Success) check(second.value).value else firstChecked.value
    return fallback?.let { AnalysisResult.Success(it) } ?: AnalysisResult.Failed(AnalysisError.Rejected)
}

private fun String.squashSpaces() = trim().replace(WHITESPACE, " ")

private val WHITESPACE = Regex("""\s+""")

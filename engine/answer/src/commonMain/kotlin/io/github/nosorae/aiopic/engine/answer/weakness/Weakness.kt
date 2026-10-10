package io.github.nosorae.aiopic.engine.answer.weakness

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import kotlin.math.roundToInt

/** 사용자가 "틀림"을 누른 교정 하나. 답변 id 와 `Feedback.corrections` 안의 순서로 가리킨다(F8.1). */
data class CorrectionFlag(val answerId: String, val index: Int)

/** 문항 유형 하나의 약점 점수(0~100). 낮을수록 약하다. 출제(F6.1)와 기록(F7.3)이 읽는다. */
data class TypeWeakness(val type: QuestionType, val score: Int)

/**
 * 분석이 끝난 답변만 문항 유형별로 묶어 평균 점수를 낸다. 약한 유형부터 정렬한다.
 * 분석 대기와 평가할 수 없음은 점수가 없어서 세지 않는다. 회차별 약점은 그 회차 답변만 넘긴다.
 *
 * 틀림으로 표시한 교정은 그 교정이 깎은 점수만큼 답변 점수를 되돌린다(F8.2, ADR 0027). 표시한 교정에 깎은 점수가 없으면
 * (그 값을 받기 전에 저장한 답변이거나 출력 검사가 버린 값) 교정 하나를 점수로 바꿀 근거가 없어서 그 답변을 평균에서 뺀다.
 * 모든 답변이 빠진 유형은 결과에 없다.
 */
fun weaknessScores(answers: List<Answer>, flagged: Set<CorrectionFlag> = emptySet()): List<TypeWeakness> = answers
    .mapNotNull { answer ->
        val done = answer.analysis as? AnswerAnalysis.Done ?: return@mapNotNull null
        val wrong = flagged.filter { it.answerId == answer.id }.mapNotNull { done.feedback.corrections.getOrNull(it.index) }
        val restored = wrong.map { it.deducted ?: return@mapNotNull null }.sum()
        answer.question.type to (done.feedback.score + restored).coerceAtMost(100)
    }
    .groupBy({ it.first }, { it.second })
    .map { (type, scores) -> TypeWeakness(type, scores.average().roundToInt()) }
    .sortedWith(compareBy({ it.score }, { it.type.id }))

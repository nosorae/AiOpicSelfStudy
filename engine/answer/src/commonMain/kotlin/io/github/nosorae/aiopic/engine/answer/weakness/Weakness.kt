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
 * 틀림 표시가 하나라도 붙은 답변은 평균에서 뺀다(F8.2). 답변 프롬프트는 점수를 "문항 유형에 얼마나
 * 맞는 답인가"로만 정하고 교정 수와 점수를 잇지 않아서, 교정 하나를 점수 몇 점으로 바꿀 근거가 없다.
 * 그래서 교정을 잘못 짚은 답변은 모델 판단 전체를 믿지 않는다. 모든 답변이 빠진 유형은 결과에 없다.
 */
// ponytail: 표시 하나로 답변 하나를 통째로 뺀다. 프롬프트가 교정 하나에 깎는 폭을 정하면 그만큼만 되돌린다.
fun weaknessScores(answers: List<Answer>, flagged: Set<CorrectionFlag> = emptySet()): List<TypeWeakness> = answers
    .mapNotNull { answer ->
        val done = answer.analysis as? AnswerAnalysis.Done ?: return@mapNotNull null
        if (flagged.any { it.answerId == answer.id && it.index in done.feedback.corrections.indices }) return@mapNotNull null
        answer.question.type to done.feedback.score
    }
    .groupBy({ it.first }, { it.second })
    .map { (type, scores) -> TypeWeakness(type, scores.average().roundToInt()) }
    .sortedWith(compareBy({ it.score }, { it.type.id }))

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
 * 틀림으로 표시한 교정 하나당 돌려주는 점수. 모델 점수는 교정할 곳을 찾은 만큼 깎여 있으니,
 * 잘못 짚은 교정만큼 되돌려 그 교정이 약점 점수에 들어가지 않게 한다(F8.2).
 */
// ponytail: 교정 하나에 고정 5점. 쌓인 표시(F8.4)로 모델이 교정 하나에 깎는 폭을 재면 그 값으로 바꾼다.
const val RESTORED_PER_FLAG = 5

/**
 * 분석이 끝난 답변만 문항 유형별로 묶어 평균 점수를 낸다. 약한 유형부터 정렬한다.
 * 분석 대기와 평가할 수 없음은 점수가 없어서 세지 않는다. 회차별 약점은 그 회차 답변만 넘긴다.
 */
fun weaknessScores(answers: List<Answer>, flagged: Set<CorrectionFlag> = emptySet()): List<TypeWeakness> = answers
    .mapNotNull { answer ->
        val done = answer.analysis as? AnswerAnalysis.Done ?: return@mapNotNull null
        val wrong = flagged.count { it.answerId == answer.id && it.index in done.feedback.corrections.indices }
        answer.question.type to (done.feedback.score + wrong * RESTORED_PER_FLAG).coerceAtMost(100)
    }
    .groupBy({ it.first }, { it.second })
    .map { (type, scores) -> TypeWeakness(type, scores.average().roundToInt()) }
    .sortedWith(compareBy({ it.score }, { it.type.id }))

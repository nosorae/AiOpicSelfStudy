package io.github.nosorae.aiopic.ui.featurepractice

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.weakness.weaknessScores
import io.github.nosorae.aiopic.engine.question.model.Question

/**
 * 한 문항 연습에 낼 질문 하나를 고른다(F4.1). 약점 점수가 가장 낮은 유형에서 내고, 그 유형 문항이 풀에 없으면 다음 약한 유형으로 간다.
 * 분석이 끝난 답변이 없으면 풀 전체에서 고른다. 같은 유형 안에서는 아직 답하지 않은 질문을 먼저 낸다. 풀이 비었으면 null 이다.
 */
// ponytail: 늘 가장 약한 유형만 낸다. 한 유형만 계속 나와 지루하다는 말이 나오면 점수에 반비례한 가중치로 고른다.
@Inject
class PickPracticeQuestionUseCase(private val repository: PracticeRepository) {
    suspend operator fun invoke(): Question? {
        val pool = repository.pool()
        val answers = repository.answers()
        val candidates = weaknessScores(answers, repository.flaggedCorrections())
            .firstNotNullOfOrNull { weak -> pool.filter { it.type == weak.type }.ifEmpty { null } }
            ?: pool
        val answered = answers.map { it.question.id }.toSet()
        return candidates.filter { it.id !in answered }.ifEmpty { candidates }.randomOrNull()
    }
}

package io.github.nosorae.aiopic.ui.featureexam

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.question.model.SurveyAnswers

/** 낸 서베이 답을 저장한다. 다음 모의고사 서베이를 미리 채우고(F2.2) 한 문항 연습이 고른 주제의 질문을 먼저 낸다. */
@Inject
class SaveSurveyAnswersUseCase(private val store: SurveyStore) {
    suspend operator fun invoke(answers: SurveyAnswers) = store.saveSurveyAnswers(answers)
}

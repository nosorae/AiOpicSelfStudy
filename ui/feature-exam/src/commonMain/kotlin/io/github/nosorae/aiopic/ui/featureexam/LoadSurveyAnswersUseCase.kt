package io.github.nosorae.aiopic.ui.featureexam

import dev.zacsweers.metro.Inject

/** 저장한 서베이 답으로 채운 서베이 상태. 낸 적이 없으면 빈 서베이다. */
@Inject
class LoadSurveyAnswersUseCase(private val store: SurveyStore, private val content: ExamContent) {
    suspend operator fun invoke(): SurveyState = SurveyState.prefilled(content.survey, store.surveyAnswers())
}

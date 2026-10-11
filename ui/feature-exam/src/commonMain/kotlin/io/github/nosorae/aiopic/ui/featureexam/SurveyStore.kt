package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.question.model.SurveyAnswers

/**
 * 마지막으로 낸 서베이 답 하나를 둔다. 모의고사 서베이와 설정의 서베이가 같은 값을 읽고 바꾼다.
 * 앱 층은 설정 저장소로, 테스트는 메모리로 구현한다.
 */
interface SurveyStore {
    /** 아직 낸 적이 없으면 null 이다. */
    suspend fun surveyAnswers(): SurveyAnswers?

    suspend fun saveSurveyAnswers(answers: SurveyAnswers)
}

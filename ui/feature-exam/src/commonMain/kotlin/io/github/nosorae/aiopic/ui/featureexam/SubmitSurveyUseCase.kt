package io.github.nosorae.aiopic.ui.featureexam

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.question.model.SurveyAnswers
import io.github.nosorae.aiopic.engine.session.model.ExamEvent
import io.github.nosorae.aiopic.engine.session.model.ExamSession

/**
 * 모의시험 서베이를 낸다. 답 전체를 저장하고(F2.2) 회차에는 고른 주제만 넘겨 다음 단계로 간다.
 * 출제는 주제만 보고 Part 1~3 답은 저장한 서베이 답에만 남는다.
 */
@Inject
class SubmitSurveyUseCase(
    private val saveSurveyAnswers: SaveSurveyAnswersUseCase,
    private val applyExamEvent: ApplyExamEventUseCase,
) {
    suspend operator fun invoke(session: ExamSession, answers: SurveyAnswers): ExamSession {
        saveSurveyAnswers(answers)
        return applyExamEvent(session, ExamEvent.SurveySubmitted(answers.topics))
    }
}

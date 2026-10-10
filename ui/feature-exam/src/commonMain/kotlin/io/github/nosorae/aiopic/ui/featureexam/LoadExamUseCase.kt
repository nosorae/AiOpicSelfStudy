package io.github.nosorae.aiopic.ui.featureexam

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.question.model.SurveyAnswers
import io.github.nosorae.aiopic.engine.session.model.ExamSession

/** 회차를 읽는다. 없으면 새로 만들어 저장한다. 서베이는 저장한 답으로 채우고, 이 회차에서 이미 낸 주제가 있으면 그 주제로 채운다(F2.2). */
@Inject
class LoadExamUseCase(
    private val repository: ExamRepository,
    private val surveyStore: SurveyStore,
    private val content: ExamContent,
) {
    suspend operator fun invoke(id: String): LoadedExam {
        val session = repository.session(id) ?: ExamSession(id).also { repository.save(it) }
        val saved = surveyStore.surveyAnswers()
        val answers = if (session.surveyTopics.isEmpty()) saved else SurveyAnswers(saved?.choices.orEmpty(), session.surveyTopics)
        return LoadedExam(session, content, SurveyState.prefilled(content.survey, answers))
    }
}

data class LoadedExam(val session: ExamSession, val content: ExamContent, val survey: SurveyState)

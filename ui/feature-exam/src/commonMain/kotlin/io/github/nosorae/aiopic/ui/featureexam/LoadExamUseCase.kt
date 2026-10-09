package io.github.nosorae.aiopic.ui.featureexam

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.session.model.ExamSession

/** 회차를 읽는다. 없으면 새로 만들어 저장한다. 서베이는 이 회차에서 고른 것, 없으면 지난 회차의 선택으로 채운다(F2.2). */
@Inject
class LoadExamUseCase(
    private val repository: ExamRepository,
    private val content: ExamContent,
) {
    suspend operator fun invoke(id: String): LoadedExam {
        val session = repository.session(id) ?: ExamSession(id).also { repository.save(it) }
        val topics = session.surveyTopics.ifEmpty { repository.lastSurveyTopics() }.filter { it in content.topics }
        return LoadedExam(session, content, topics)
    }
}

data class LoadedExam(val session: ExamSession, val content: ExamContent, val prefilledTopics: List<String>)

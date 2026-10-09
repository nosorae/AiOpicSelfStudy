package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** 저장한 순서를 그대로 돌려주는 메모리 저장소. 값을 바꾸면 구독한 화면에 바로 흘러간다. */
class FakeReportStore(sessions: List<ExamSession> = emptyList(), answers: List<Answer> = emptyList()) : ReportStore {
    val sessions = MutableStateFlow(sessions)
    val answers = MutableStateFlow(answers)

    override fun session(id: String): Flow<ExamSession?> = sessions.map { list -> list.firstOrNull { it.id == id } }

    override fun answers(examSessionId: String): Flow<List<Answer>> = answers.map { list -> list.filter { it.examSessionId == examSessionId } }

    override fun answers(): Flow<List<Answer>> = answers
}

package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** 저장한 순서를 그대로 돌려주는 메모리 저장소. 값을 바꾸면 구독한 화면에 바로 흘러간다. */
class FakeReportStore(
    sessions: List<ExamSession> = emptyList(),
    answers: List<Answer> = emptyList(),
    /** 본 표시. 앱을 다시 켠 경우는 앞 저장소의 이 값만 넘겨 새로 만든다. */
    val seen: MutableMap<String, Int> = mutableMapOf(),
) : ReportStore {
    val sessions = MutableStateFlow(sessions)
    val answers = MutableStateFlow(answers)

    override fun sessions(): Flow<List<ExamSession>> = sessions

    override fun seen(): Flow<Map<String, Int>> = flowOf(seen.toMap())

    override suspend fun markSeen(key: String, value: Int) {
        seen[key] = value
    }

    override fun answers(examSessionId: String): Flow<List<Answer>> = answers.map { list -> list.filter { it.examSessionId == examSessionId } }

    override fun answers(): Flow<List<Answer>> = answers

    val jobs = MutableStateFlow(emptyList<AnalysisJob>())

    override fun jobs(): Flow<List<AnalysisJob>> = jobs

    /** 다시 보내 달라고 받은 답변 id 와 `session:` 을 붙인 회차 id. */
    val retried = mutableListOf<String>()

    override suspend fun retryAnswer(answerId: String) {
        retried += answerId
    }

    override suspend fun retrySession(sessionId: String) {
        retried += "session:$sessionId"
    }
}

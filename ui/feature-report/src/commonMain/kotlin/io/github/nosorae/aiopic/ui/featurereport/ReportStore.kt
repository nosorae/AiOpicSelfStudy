package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import kotlinx.coroutines.flow.Flow

/**
 * 리포트가 읽는 저장소와 다시 분석하기. 앱 층이 답변 저장소(OpicStore)를 그대로 이어 주고, 테스트는 메모리 가짜를 쓴다.
 * 답변 분석이 끝나면 저장소가 새 값을 흘려서 리포트가 바로 바뀐다.
 */
interface ReportStore {
    /** 저장한 순서의 모든 회차. 리포트는 그중 한 회차와 바로 앞 회차 등급을 본다(#198). */
    fun sessions(): Flow<List<ExamSession>>

    fun answers(examSessionId: String): Flow<List<Answer>>

    fun answers(): Flow<List<Answer>>

    /** 분석 작업 표(#205). 기다리는 답변과 등급이 지금 무엇을 기다리는지 보인다. */
    fun jobs(): Flow<List<AnalysisJob>>

    /** 분석하지 못한 답변을 기다림으로 돌려 다시 보낸다(#203). */
    suspend fun retryAnswer(answerId: String)

    /** 회차에서 분석하지 못한 답변과 등급을 다시 보낸다(#203). */
    suspend fun retrySession(sessionId: String)

    /** 이미 본 것(#198). 축하를 한 번만 하려고 앱을 다시 켜도 남게 둔다. */
    fun seen(): Flow<Map<String, Int>>

    suspend fun markSeen(key: String, value: Int)
}

package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import kotlinx.coroutines.flow.Flow

/**
 * 리포트가 읽는 저장소. 앱 층이 답변 저장소(OpicStore)를 그대로 이어 주고, 테스트는 메모리 가짜를 쓴다.
 * 답변 분석이 끝나면 저장소가 새 값을 흘려서 리포트가 바로 바뀐다.
 */
interface ReportStore {
    fun session(id: String): Flow<ExamSession?>

    fun answers(examSessionId: String): Flow<List<Answer>>

    fun answers(): Flow<List<Answer>>
}

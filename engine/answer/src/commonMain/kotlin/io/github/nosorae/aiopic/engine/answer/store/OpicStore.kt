package io.github.nosorae.aiopic.engine.answer.store

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * 회차 상태와 답변 저장소. 회차 상태는 바뀔 때마다 통째로 저장하고, 화면은 여기서 구독한 값으로 그린다(F3.1, ADR 0012).
 * 리포트와 기록(F7)은 회차별, 전체 답변을 저장한 순서대로 읽는다. 틀림 표시(F8.1)도 여기 둔다.
 */
class OpicStore(db: OpicDatabase) {
    private val dao = db.dao()

    suspend fun save(session: ExamSession) = dao.saveSession(session.id, Json.encodeToString(session))

    fun session(id: String): Flow<ExamSession?> = dao.session(id).map { it?.let { row -> Json.decodeFromString<ExamSession>(row.json) } }

    fun sessions(): Flow<List<ExamSession>> = dao.sessions().map { rows -> rows.map { Json.decodeFromString<ExamSession>(it.json) } }

    suspend fun save(answer: Answer) = dao.saveAnswer(answer.id, answer.examSessionId, Json.encodeToString(answer))

    fun answers(examSessionId: String): Flow<List<Answer>> = dao.answers(examSessionId).map(::decodeAnswers)

    fun answers(): Flow<List<Answer>> = dao.answers().map(::decodeAnswers)

    fun flags(): Flow<Set<CorrectionFlag>> = dao.flags().map { rows -> rows.map { CorrectionFlag(it.answerId, it.index) }.toSet() }

    /** [wrong] 이면 표시를 남기고 아니면 지운다. 같은 표시를 두 번 남겨도 하나다. */
    suspend fun mark(flag: CorrectionFlag, wrong: Boolean) = if (wrong) dao.addFlag(flag.answerId, flag.index) else dao.removeFlag(flag.answerId, flag.index)

    private fun decodeAnswers(rows: List<AnswerRow>) = rows.map { Json.decodeFromString<Answer>(it.json) }
}

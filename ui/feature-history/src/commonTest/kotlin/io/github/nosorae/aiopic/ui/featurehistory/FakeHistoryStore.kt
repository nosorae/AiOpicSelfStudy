package io.github.nosorae.aiopic.ui.featurehistory

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeHistoryStore(sessions: List<ExamSession> = emptyList(), answers: List<Answer> = emptyList()) : HistoryStore {
    val sessions = MutableStateFlow(sessions)
    val answers = MutableStateFlow(answers)
    val flags = MutableStateFlow(emptySet<CorrectionFlag>())

    override fun sessions(): Flow<List<ExamSession>> = sessions

    override fun answers(): Flow<List<Answer>> = answers

    override fun flags(): Flow<Set<CorrectionFlag>> = flags
}

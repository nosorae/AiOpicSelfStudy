package io.github.nosorae.aiopic.ui.featurehistory

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import kotlinx.coroutines.flow.Flow

/** 기록이 읽는 저장소. 앱 층이 답변 저장소(OpicStore)를 그대로 이어 주고, 테스트는 메모리 가짜를 쓴다. 저장한 순서로 돌려준다. */
interface HistoryStore {
    fun sessions(): Flow<List<ExamSession>>

    fun answers(): Flow<List<Answer>>

    /** 리포트에서 "틀림"을 누른 교정. 약점 점수에서 뺀다(F8.2). */
    fun flags(): Flow<Set<CorrectionFlag>>
}

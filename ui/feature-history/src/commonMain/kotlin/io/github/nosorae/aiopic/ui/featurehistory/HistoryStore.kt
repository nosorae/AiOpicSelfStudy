package io.github.nosorae.aiopic.ui.featurehistory

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import kotlinx.coroutines.flow.Flow

/** 기록이 읽는 저장소. 앱 층이 답변 저장소(OpicStore)를 그대로 이어 주고, 테스트는 메모리 가짜를 쓴다. 저장한 순서로 돌려준다. */
interface HistoryStore {
    fun sessions(): Flow<List<ExamSession>>

    fun answers(): Flow<List<Answer>>
}

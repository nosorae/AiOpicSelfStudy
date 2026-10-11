package io.github.nosorae.aiopic.ui.featurehome

import io.github.nosorae.aiopic.engine.session.model.ExamSession
import kotlinx.coroutines.flow.Flow

/** 저장된 회차 상태를 저장한 순서대로 내보내고, 새로 시작할 때 앞 회차를 끝내 저장한다. 앱 층이 저장소를 넣고, 테스트는 메모리로 구현한다. */
interface ExamSessionSource {
    fun sessions(): Flow<List<ExamSession>>

    suspend fun save(session: ExamSession)
}

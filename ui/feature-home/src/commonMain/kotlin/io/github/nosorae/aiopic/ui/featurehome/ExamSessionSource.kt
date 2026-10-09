package io.github.nosorae.aiopic.ui.featurehome

import io.github.nosorae.aiopic.engine.session.model.ExamSession
import kotlinx.coroutines.flow.Flow

/** 저장된 회차 상태를 저장한 순서대로 내보낸다. 앱 층이 저장소를 넣고, 테스트는 람다로 목록을 정한다. */
fun interface ExamSessionSource {
    fun sessions(): Flow<List<ExamSession>>
}

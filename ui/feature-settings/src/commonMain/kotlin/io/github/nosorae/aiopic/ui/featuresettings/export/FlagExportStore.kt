package io.github.nosorae.aiopic.ui.featuresettings.export

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import kotlinx.coroutines.flow.Flow

/** 내보내기가 읽는 저장소. 앱 층이 답변 저장소와 틀림 표시 저장소를 이어 주고, 테스트는 메모리 가짜를 쓴다. 답변은 저장한 순서다. */
interface FlagExportStore {
    fun answers(): Flow<List<Answer>>

    fun flags(): Flow<Set<CorrectionFlag>>
}

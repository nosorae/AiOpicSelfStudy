package io.github.nosorae.aiopic.ui.featurereport

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.model.Answer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 답변 하나를 구독한다. 모의고사과 한 문항 연습 답변을 같은 길로 읽는다(F7.1). 없으면 null 이다. */
@Inject
class ObserveAnswerUseCase(private val store: ReportStore) {
    // ponytail: 전체 답변에서 찾는다. 답변이 수천 개로 늘면 저장소에 id 로 읽는 쿼리를 더한다.
    operator fun invoke(answerId: String): Flow<Answer?> = store.answers().map { list -> list.firstOrNull { it.id == answerId } }
}

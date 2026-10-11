package io.github.nosorae.aiopic.ui.featurereport.flag

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.JobKind
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog
import io.github.nosorae.aiopic.ui.featurereport.ObserveAnswerUseCase
import io.github.nosorae.aiopic.ui.featurereport.ReportNames
import io.github.nosorae.aiopic.ui.featurereport.ReportStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 답변과 그 답변에서 "틀림"을 누른 교정의 `Feedback.corrections` 안 순서(F8.1). 답변이 없으면 [answer] 는 null 이다.
 * [job] 은 답변의 분석 작업(#205)으로, 기다리는 동안 무엇을 기다리는지 보인다.
 */
data class AnswerReport(val answer: Answer?, val wrongCorrections: Set<Int>, val names: ReportNames, val job: AnalysisJob? = null)

/** 답변 리포트 화면(S11)이 구독하는 답변과 "틀림" 표시를 한 흐름으로 낸다. */
@Inject
class ObserveAnswerReportUseCase(
    private val observeAnswer: ObserveAnswerUseCase,
    private val store: FlagStore,
    private val reports: ReportStore,
    private val modelCatalog: ModelCatalog,
) {
    operator fun invoke(answerId: String): Flow<AnswerReport> = combine(observeAnswer(answerId), store.flags(), reports.jobs()) { answer, flags, jobs ->
        AnswerReport(
            answer,
            flags.filter { it.answerId == answerId }.map { it.index }.toSet(),
            ReportNames(models = modelCatalog.models),
            jobs.firstOrNull { it.kind == JobKind.Answer && it.targetId == answerId },
        )
    }
}

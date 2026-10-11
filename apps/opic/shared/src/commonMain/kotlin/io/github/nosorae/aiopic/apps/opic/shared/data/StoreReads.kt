package io.github.nosorae.aiopic.apps.opic.shared.data

import io.github.nosorae.aiopic.engine.answer.analysis.AnalysisJobs
import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.ui.featurehistory.HistoryStore
import io.github.nosorae.aiopic.ui.featurehome.ExamSessionSource
import io.github.nosorae.aiopic.ui.featurereport.ReportStore
import io.github.nosorae.aiopic.ui.featurereport.flag.FlagStore
import io.github.nosorae.aiopic.ui.featuresettings.export.FlagExportStore
import kotlinx.coroutines.flow.Flow

/**
 * 홈, 리포트, 기록, 내보내기가 구독하는 저장소(F7, F8.1, F8.3). 리포트와 기록은 분석 작업 표(#205)도 구독하고, 다시 분석하기(#203)는 작업을 다시 넣는다. 다섯 인터페이스 모두 [OpicStore] 를 그대로 흘려 보내서
 * 한 클래스가 맡는다. 분석이 끝나 답변이 바뀌면 구독한 화면이 바로 새 값을 받는다.
 */
class StoreReads(private val store: OpicStore, private val jobs: AnalysisJobs) :
    ExamSessionSource,
    ReportStore,
    HistoryStore,
    FlagStore,
    FlagExportStore {
    override fun sessions(): Flow<List<ExamSession>> = store.sessions()

    override fun session(id: String): Flow<ExamSession?> = store.session(id)

    override fun answers(examSessionId: String): Flow<List<Answer>> = store.answers(examSessionId)

    override fun answers(): Flow<List<Answer>> = store.answers()

    override fun flags(): Flow<Set<CorrectionFlag>> = store.flags()

    override suspend fun mark(flag: CorrectionFlag, wrong: Boolean) = store.mark(flag, wrong)

    override fun jobs(): Flow<List<AnalysisJob>> = store.jobs()

    override suspend fun retryAnswer(answerId: String) = jobs.retryAnswer(answerId)

    override suspend fun retrySession(sessionId: String) = jobs.retrySession(sessionId)
}

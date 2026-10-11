package io.github.nosorae.aiopic.ui.featurereport

import dev.zacsweers.metro.Inject

/** 회차에서 분석하지 못한 답변과 등급을 한 번에 다시 보낸다(#203). */
@Inject
class RetrySessionAnalysisUseCase(private val store: ReportStore) {
    suspend operator fun invoke(sessionId: String) = store.retrySession(sessionId)
}

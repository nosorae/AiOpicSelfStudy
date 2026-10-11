package io.github.nosorae.aiopic.ui.featurereport

import dev.zacsweers.metro.Inject

/** 분석하지 못한 답변을 다시 보낸다(#203). 답변이 기다림으로 바뀌어 화면이 바로 모이는 점으로 돌아간다. */
@Inject
class RetryAnswerAnalysisUseCase(private val store: ReportStore) {
    suspend operator fun invoke(answerId: String) = store.retryAnswer(answerId)
}

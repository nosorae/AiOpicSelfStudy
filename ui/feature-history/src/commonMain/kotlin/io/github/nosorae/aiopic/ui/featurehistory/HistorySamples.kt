package io.github.nosorae.aiopic.ui.featurehistory

import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.SessionGrade

/** 미리보기와 스크린샷 테스트가 같이 쓰는 고정 상태. */
internal object HistorySamples {
    private const val FLASH = "Gemini 3.8 Flash"
    private const val LITE = "Gemini 3.5 Flash Lite"

    private fun session(id: String, low: String, high: String, status: ExamStatus = ExamStatus.Finished) = ExamSession(id = id, status = status, grade = SessionGrade(emptyList(), low, high, emptyList(), emptyList()))

    val filled = HistoryUiState(
        loading = false,
        sessions = listOf(
            HistorySession(session("s1", "IM1", "IM2"), listOf(FLASH)),
            HistorySession(session("s2", "IM2", "IM3", ExamStatus.TimeUp), listOf(FLASH, LITE)),
            HistorySession(session("s3", "IH", "IH"), listOf(FLASH)),
            HistorySession(ExamSession(id = "s4", status = ExamStatus.Finished), emptyList()),
        ),
        weaknesses = listOf(
            HistoryWeakness("롤플레이 문제 해결", 48),
            HistoryWeakness("과거 경험", 61),
            HistoryWeakness("습관", 77),
        ),
    )

    val empty = HistoryUiState(loading = false)
}

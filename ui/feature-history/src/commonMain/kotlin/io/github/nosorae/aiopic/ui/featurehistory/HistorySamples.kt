package io.github.nosorae.aiopic.ui.featurehistory

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.weakness.TypeWeakness
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.SessionGrade

/** 미리보기와 스크린샷 테스트가 같이 쓰는 고정 상태. */
internal object HistorySamples {
    private val flash = AnalyzedBy("gemini", "gemini-3.8-flash", "v1")
    private val lite = AnalyzedBy("gemini", "gemini-3.5-flash-lite", "v1")

    private fun session(id: String, low: String, high: String, status: ExamStatus = ExamStatus.Finished) = ExamSession(id = id, status = status, grade = SessionGrade(emptyList(), low, high, emptyList(), emptyList()))

    val filled = HistoryUiState(
        loading = false,
        sessions = listOf(
            HistorySession(session("s1", "IM1", "IM2"), listOf(flash)),
            HistorySession(session("s2", "IM2", "IM3", ExamStatus.TimeUp), listOf(flash, lite)),
            HistorySession(session("s3", "IH", "IH"), listOf(flash)),
            HistorySession(ExamSession(id = "s4", status = ExamStatus.Finished), emptyList()),
        ),
        weaknesses = listOf(
            TypeWeakness(QuestionType("roleplay-solve"), 48),
            TypeWeakness(QuestionType("past-experience"), 61),
            TypeWeakness(QuestionType("routine"), 77),
        ),
    )

    val empty = HistoryUiState(loading = false)
}

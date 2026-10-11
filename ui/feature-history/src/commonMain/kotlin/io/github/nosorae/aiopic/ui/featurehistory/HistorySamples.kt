package io.github.nosorae.aiopic.ui.featurehistory

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.JobState
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.SessionGrade

/** 미리보기와 스크린샷 테스트가 같이 쓰는 고정 상태. 목록은 최근 것부터다. */
internal object HistorySamples {
    private const val FLASH = "Gemini 3.8 Flash"
    private const val LITE = "Gemini 3.5 Flash Lite"

    private fun session(id: String, low: String, high: String, status: ExamStatus = ExamStatus.Finished) = ExamSession(id = id, status = status, grade = SessionGrade(emptyList(), low, high, emptyList(), emptyList()))

    private fun scored(score: Int) = AnswerAnalysis.Done(
        AnalyzedBy("gemini", "flash", "v1"),
        Feedback("", emptyList(), emptyList(), true, 0, 0.0, emptyList(), null, Pronunciation.NotEvaluated, score),
    )

    val filled = HistoryUiState(
        loading = false,
        entries = listOf(
            HistoryPractice("p3", "롤플레이 문제 해결", null, AnswerAnalysis.Pending, JobState.Running),
            HistorySession(ExamSession(id = "s4", status = ExamStatus.Finished), listOf(FLASH), answered = 7, analyzed = 4, number = 4),
            HistoryPractice("p2", "과거 경험", FLASH, scored(72)),
            HistorySession(session("s3", "IH", "IH"), listOf(FLASH), number = 3),
            HistoryPractice("p1", "습관", null, AnswerAnalysis.Failed(AnalysisError.Retryable)),
            HistorySession(session("s2", "IM2", "IM3", ExamStatus.TimeUp), listOf(FLASH, LITE), number = 2),
            HistorySession(session("s1", "IM1", "IM2"), listOf(FLASH), number = 1),
        ),
        weaknesses = listOf(
            HistoryWeakness("롤플레이 문제 해결", 48),
            HistoryWeakness("과거 경험", 61),
            HistoryWeakness("습관", 77),
        ),
        grades = listOf("NL", "NM", "NH", "IL", "IM1", "IM2", "IM3", "IH", "AL"),
    )

    val empty = HistoryUiState(loading = false)
}

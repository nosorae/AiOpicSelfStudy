package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.JobKind
import io.github.nosorae.aiopic.engine.answer.model.JobState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AnalysisNoticeTest {
    private val start = Instant.fromEpochSeconds(1_000)
    private fun job(id: String, state: JobState, at: Long, kind: JobKind = JobKind.Answer) = AnalysisJob(kind, id, state, updatedAt = Instant.fromEpochSeconds(at))

    @Test
    fun `229 진행은 비우기를 시작한 뒤 끝난 작업 수와 남은 작업 수로 센다`() {
        val jobs = listOf(
            job("old", JobState.Succeeded, at = 10),
            job("a1", JobState.Succeeded, at = 1_001),
            job("a2", JobState.Failed, at = 1_002),
            job("a3", JobState.Running, at = 1_003),
            job("a4", JobState.Queued, at = 500),
        )

        assertEquals(AnalysisProgress(done = 2, total = 4), analysisProgress(jobs, since = start))
    }

    @Test
    fun `229 남은 작업이 없으면 진행을 내린다`() {
        assertNull(analysisProgress(listOf(job("a1", JobState.Succeeded, at = 1_001)), since = start))
    }

    @Test
    fun `229 모의고사 답변이 끝나면 회차 리포트로 연습 답변이면 답변 리포트로 보낸다`() {
        val sessionOf = mapOf("exam-answer" to "s1")::get

        assertEquals(
            AnalysisOutcome(AppRoute.ReportDetail("s1"), failed = false),
            analysisOutcome(listOf(job("exam-answer", JobState.Succeeded, at = 1_001)), start, sessionOf),
        )
        assertEquals(
            AnalysisOutcome(AppRoute.AnswerReport("practice"), failed = false),
            analysisOutcome(listOf(job("practice", JobState.Succeeded, at = 1_001)), start, sessionOf),
        )
        assertEquals(
            AnalysisOutcome(AppRoute.ReportDetail("s1"), failed = false),
            analysisOutcome(listOf(job("exam-answer", JobState.Succeeded, at = 1_001), job("s1", JobState.Succeeded, at = 1_002, JobKind.Grade)), start, sessionOf),
        )
    }

    @Test
    fun `229 하나라도 실패로 끝났으면 실패한 작업의 리포트로 실패를 알린다`() {
        val jobs = listOf(job("p1", JobState.Failed, at = 1_001), job("p2", JobState.Succeeded, at = 1_002))

        assertEquals(AnalysisOutcome(AppRoute.AnswerReport("p1"), failed = true), analysisOutcome(jobs, start) { null })
    }

    @Test
    fun `229 남은 작업이 있거나 이번에 끝난 작업이 없으면 알리지 않는다`() {
        assertNull(analysisOutcome(listOf(job("a1", JobState.Succeeded, at = 1_001), job("a2", JobState.RetryScheduled, at = 1_002)), start) { null })
        assertNull(analysisOutcome(listOf(job("a1", JobState.Succeeded, at = 10)), start) { null })
    }
}

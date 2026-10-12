package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.JobKind
import io.github.nosorae.aiopic.engine.answer.model.JobState
import kotlin.time.Instant

/**
 * 앱 밖에서 보이는 분석 진행(#229). Android 는 포그라운드 서비스 알림, iOS 26 이상은 시스템 진행 표시에 쓴다.
 * 상태는 분석 작업 표에서만 읽는다(ADR 0032).
 */
data class AnalysisProgress(val done: Int, val total: Int) {
    val title: String get() = "답변을 분석하고 있어요"

    /** 기다리면 무엇이 나오는지와 몇 개 남았는지를 함께 쓴다. */
    val text: String get() = "$total 개 중 $done 개 끝. 문법 교정과 한 단계 위 표현이 곧 나와요"
}

/** 앱이 화면에 없을 때 분석이 끝나면 띄우는 알림. 누르면 [route] 로 간다. */
data class AnalysisOutcome(val route: AppRoute, val failed: Boolean) {
    val title: String get() = if (failed) "분석하지 못했어요" else "리포트가 준비됐어요"

    val text: String get() = if (failed) "리포트에서 이유를 보고 다시 분석할 수 있어요" else "문법 교정과 한 단계 위 표현을 볼 수 있어요"
}

/**
 * [since] 에 비우기를 시작한 뒤의 진행. 그 뒤 끝난 작업과 아직 남은 작업을 센다. 남은 작업이 없으면 null 이다.
 * ponytail: 비우기 한 번을 한 묶음으로 센다. 다시 보내기로 비우기가 나뉘면 뒤 비우기는 남은 것만 센다.
 */
fun analysisProgress(jobs: List<AnalysisJob>, since: Instant): AnalysisProgress? {
    val left = jobs.count { it.state.active }
    if (left == 0) return null
    val done = jobs.count { !it.state.active && it.updatedAt >= since }
    return AnalysisProgress(done, done + left)
}

/**
 * [since] 에 시작한 비우기가 끝난 뒤 알릴 결과. 남은 작업이 있거나 그 뒤 끝난 작업이 없으면 null 이다.
 * 실패가 있으면 실패한 작업의, 없으면 마지막에 끝난 작업의 리포트로 보낸다. 모의고사 답변은 회차 리포트로 간다.
 * [sessionOf] 는 답변 id 로 회차 id 를 찾는다. 연습 답변이면 null 이다.
 */
fun analysisOutcome(jobs: List<AnalysisJob>, since: Instant, sessionOf: (String) -> String?): AnalysisOutcome? {
    if (jobs.any { it.state.active }) return null
    val finished = jobs.filter { it.updatedAt >= since }
    val job = finished.firstOrNull { it.state == JobState.Failed } ?: finished.maxByOrNull { it.updatedAt } ?: return null
    val route = when (job.kind) {
        JobKind.Grade -> AppRoute.ReportDetail(job.targetId)
        JobKind.Answer -> sessionOf(job.targetId)?.let { AppRoute.ReportDetail(it) } ?: AppRoute.AnswerReport(job.targetId)
    }
    return AnalysisOutcome(route, failed = job.state == JobState.Failed)
}

package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.JobKind
import io.github.nosorae.aiopic.engine.answer.model.JobState
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration
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

/** 지금부터 시작하는 비우기의 진행. 작업 표가 바뀔 때마다 새 값을 낸다. */
fun OpicStore.analysisProgress(since: Instant = now()): Flow<AnalysisProgress?> = jobs().map { analysisProgress(it, since) }.distinctUntilChanged()

/** 기다리거나 보내는 분석 작업이 있는가. 첫 분석을 시작할 때 알림 권한을 묻는 데 쓴다. */
fun OpicStore.analyzing(): Flow<Boolean> = jobs().map { jobs -> jobs.any { it.state.active } }.distinctUntilChanged()

/**
 * 분석 작업 표를 [drain] 으로 한 번 비우고, 그 사이 끝난 작업이 있고 남은 작업이 없을 때 앱이 화면에 없으면([visible] 이 false) [notify] 로 결과를 알린다.
 * 플랫폼 대기열의 비우기 자리에 넣는다.
 */
suspend fun OpicStore.drainAndNotify(drain: suspend () -> Duration?, visible: () -> Boolean, notify: (AnalysisOutcome) -> Unit): Duration? {
    val since = now()
    val next = drain()
    if (!visible()) {
        val sessions = answers().first().associate { it.id to it.examSessionId }
        analysisOutcome(jobs().first(), since, sessions::get)?.let(notify)
    }
    return next
}

/** 작업 표는 시각을 밀리초로 저장한다. 시작 시각도 밀리초로 잘라야 같은 밀리초에 끝난 작업을 놓치지 않는다. */
private fun now(): Instant = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())

/** 알림에 실어 보낼 경로. 누르면 [routeFromJson] 으로 되읽는다. */
val AnalysisOutcome.routeJson: String get() = Json.encodeToString(AppRoute.serializer(), route)

/** 알림에서 받은 경로. 앱 밖에서 온 값이라 읽지 못하면 null 이다. */
fun routeFromJson(json: String): AppRoute? = runCatching { Json.decodeFromString(AppRoute.serializer(), json) }.getOrNull()

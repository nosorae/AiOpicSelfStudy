package io.github.nosorae.aiopic.apps.opic.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.ui.featureexam.ExamRoute
import io.github.nosorae.aiopic.ui.featurehistory.HistoryRoute
import io.github.nosorae.aiopic.ui.featurehome.HomeRoute
import io.github.nosorae.aiopic.ui.featureonboarding.consent.ConsentRoute
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyEntryRoute
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider
import io.github.nosorae.aiopic.ui.featurepractice.PracticeRoute
import io.github.nosorae.aiopic.ui.featurereport.AnswerReportRoute
import io.github.nosorae.aiopic.ui.featurereport.SessionReportRoute
import io.github.nosorae.aiopic.ui.featuresettings.SettingsRoute
import io.github.nosorae.aiopic.ui.featuresettings.export.FlagExportRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 경로마다 그릴 화면. 두 플랫폼 앱 틀이 같이 쓴다. [onOpen] 은 지금 탭의 백스택에 화면을 쌓고,
 * [onExit] 은 시험처럼 뒤로가기를 막은 화면이 스스로 나갈 때 탭의 첫 화면까지 돌아간다.
 * [onBack] 은 키 저장, 동의처럼 일을 마친 하위 화면이 한 칸 돌아갈 때 쓴다.
 * [onRestart] 는 첫 실행 화면(S1, S2)을 마쳤을 때 백스택 전체를 시작 분기를 다시 한 첫 화면 하나로 바꾼다.
 *
 * ViewModel 은 경로 값을 키로 만든다. 같은 화면 모듈이라도 인자가 다르면 다른 ViewModel 이다.
 */
@Composable
internal fun RouteContent(
    route: AppRoute,
    graph: AppGraph,
    onOpen: (AppRoute) -> Unit,
    onExit: () -> Unit,
    onBack: () -> Unit,
    onRestart: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val restart: () -> Unit = { scope.launch { onRestart(AppRoute.start(graph.decideStartUseCase())) } }
    when (route) {
        AppRoute.Home -> HomeRoute(
            viewModel = viewModel { graph.homeViewModel },
            // 진행 중인 회차가 있으면 그 회차를 잇고, 없으면 새 회차를 연다(F3). 홈의 이어 보기 버튼과 같은 기준이다.
            onExamClick = {
                scope.launch {
                    val inProgress = graph.examSessionSource.sessions().first().lastOrNull { it.status == ExamStatus.InProgress }
                    onOpen(AppRoute.Exam(inProgress?.id ?: newId()))
                }
            },
            onPracticeClick = { onOpen(AppRoute.Practice(newId())) },
            onSessionClick = { onOpen(AppRoute.ReportDetail(it)) },
            modifier = modifier,
        )

        AppRoute.History -> HistoryRoute(
            viewModel = viewModel { graph.historyViewModel },
            onOpenSession = { onOpen(AppRoute.ReportDetail(it)) },
            modifier = modifier,
        )

        AppRoute.Settings -> SettingsRoute(
            viewModel = viewModel { graph.settingsViewModel },
            onOpenKeyEntry = { onOpen(AppRoute.KeyEntry(it.id)) },
            onOpenFreeTierNotice = { onOpen(AppRoute.FreeTierNotice) },
            onOpenFlagExport = { onOpen(AppRoute.FlagExport) },
            modifier = modifier,
        )

        is AppRoute.ReportDetail -> SessionReportRoute(
            viewModel = viewModel(key = route.toString()) { graph.sessionReportViewModelFactory.create(route.sessionId) },
            onOpenAnswer = { onOpen(AppRoute.AnswerReport(it)) },
            modifier = modifier,
        )

        is AppRoute.AnswerReport -> AnswerReportRoute(
            viewModel = viewModel(key = route.toString()) { graph.answerReportViewModelFactory.create(route.answerId) },
            modifier = modifier,
        )

        // 시험이 끝나면 시험 화면을 백스택에서 빼고 그 회차 리포트를 연다. 뒤로가기로 끝난 시험에 돌아가지 않는다.
        is AppRoute.Exam -> ExamRoute(
            viewModel = viewModel(key = route.toString()) { graph.examViewModelFactory.create(route.sessionId) },
            onExit = onExit,
            onFinish = {
                onExit()
                onOpen(AppRoute.ReportDetail(route.sessionId))
            },
            modifier = modifier,
        )

        is AppRoute.Practice -> PracticeRoute(
            viewModel = viewModel(key = route.toString()) { graph.practiceViewModelFactory.create(route.answerId) },
            onExit = onBack,
            onFeedback = {
                onBack()
                onOpen(AppRoute.AnswerReport(it))
            },
            modifier = modifier,
        )

        is AppRoute.KeyEntry -> KeyEntryRoute(
            viewModel = viewModel(key = route.toString()) {
                graph.keyEntryViewModelFactory.create(KeyProvider.entries.firstOrNull { it.id == route.provider })
            },
            onDone = onBack,
            onOpenFreeTierNotice = { onOpen(AppRoute.FreeTierNotice) },
            modifier = modifier,
        )

        AppRoute.FreeTierNotice -> ConsentRoute(viewModel = viewModel { graph.consentViewModel }, onDone = onBack, modifier = modifier)

        AppRoute.FlagExport -> FlagExportRoute(viewModel = viewModel { graph.flagExportViewModel }, modifier = modifier)

        // 첫 실행은 제공사를 고르게 둔다(null). 키를 저장하면 동의 여부를 다시 읽어 동의 화면이나 홈으로 간다.
        AppRoute.StartKeyEntry -> KeyEntryRoute(
            viewModel = viewModel(key = route.toString()) { graph.keyEntryViewModelFactory.create(null) },
            onDone = restart,
            onOpenFreeTierNotice = { onOpen(AppRoute.FreeTierNotice) },
            modifier = modifier,
        )

        AppRoute.StartConsent -> ConsentRoute(viewModel = viewModel { graph.consentViewModel }, onDone = restart, modifier = modifier)
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun newId(): String = Uuid.random().toString()

package io.github.nosorae.aiopic.apps.opic.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageButtonSize
import io.github.nosorae.aiopic.ui.design.MontageButtonType
import io.github.nosorae.aiopic.ui.design.MontageButtonVariant
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import io.github.nosorae.aiopic.ui.featurehome.HomeRoute
import io.github.nosorae.aiopic.ui.featureonboarding.consent.ConsentRoute
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyEntryRoute
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider
import io.github.nosorae.aiopic.ui.featuresettings.SettingsRoute

/**
 * 경로마다 그릴 화면. 두 플랫폼 앱 틀이 같이 쓴다. [onOpen] 은 지금 탭의 백스택에 화면을 쌓고,
 * [onExit] 은 시험처럼 뒤로가기를 막은 화면이 스스로 나갈 때 탭의 첫 화면까지 돌아간다.
 * [onBack] 은 키 저장, 동의처럼 일을 마친 하위 화면이 한 칸 돌아갈 때 쓴다.
 */
@Composable
internal fun RouteContent(
    route: AppRoute,
    graph: AppGraph,
    onOpen: (AppRoute) -> Unit,
    onExit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (route) {
        AppRoute.Home -> HomeRoute(
            viewModel = viewModel { graph.homeViewModel },
            onExamClick = { onOpen(AppRoute.Exam) },
            onPracticeClick = { onOpen(AppRoute.Practice) },
            onSessionClick = { onOpen(AppRoute.ReportDetail(it)) },
            modifier = modifier,
        )

        AppRoute.Settings -> SettingsRoute(
            viewModel = viewModel { graph.settingsViewModel },
            onOpenKeyEntry = { onOpen(AppRoute.KeyEntry(it.id)) },
            onOpenFreeTierNotice = { onOpen(AppRoute.FreeTierNotice) },
            modifier = modifier,
        )

        is AppRoute.KeyEntry -> KeyEntryRoute(
            viewModel = viewModel(key = route.provider) {
                graph.keyEntryViewModelFactory.create(KeyProvider.entries.firstOrNull { it.id == route.provider })
            },
            onDone = onBack,
            onOpenFreeTierNotice = { onOpen(AppRoute.FreeTierNotice) },
            modifier = modifier,
        )

        AppRoute.FreeTierNotice -> ConsentRoute(viewModel = viewModel { graph.consentViewModel }, onDone = onBack, modifier = modifier)

        // ponytail: 화면 노드(u-exam, u-single, u-report)가 아직 없는 경로는 제목만 그린다. 각 노드가 여기를 자기 Route 로 바꾼다.
        else -> PendingScreen(title = route.title, onExit = onExit.takeIf { route.blocksBack }, modifier = modifier)
    }
}

@Composable
private fun PendingScreen(title: String, onExit: (() -> Unit)?, modifier: Modifier = Modifier) {
    MontageScreen(
        modifier = modifier,
        topBar = {
            MontageTopBar(
                title = title,
                actions = {
                    if (onExit != null) {
                        MontageButton(
                            text = "나가기",
                            onClick = onExit,
                            variant = MontageButtonVariant.Text,
                            type = MontageButtonType.Assistive,
                            size = MontageButtonSize.Medium,
                        )
                    }
                },
            )
        },
    ) {}
}

package io.github.nosorae.aiopic.ui.featurehome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageButtonType
import io.github.nosorae.aiopic.ui.design.MontageButtonVariant
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import org.jetbrains.compose.resources.stringResource

/**
 * ViewModel 을 관측하는 바깥 겹. 어디로 갈지는 화면을 등록한 앱 틀이 람다로 정한다.
 * [onExamClick] 은 이어 볼 회차 id 를 받고, 진행 중인 회차가 없으면 null 을 받는다(F3).
 */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onExamClick: (resumeSessionId: String?) -> Unit,
    onPracticeClick: () -> Unit,
    onSessionClick: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, onExamClick, onPracticeClick, onSessionClick, modifier)
}

/** 상태만 받아 그리는 안쪽 겹. 미리보기와 스크린샷 테스트가 이 함수를 쓴다. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onExamClick: (resumeSessionId: String?) -> Unit,
    onPracticeClick: () -> Unit,
    onSessionClick: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gutter = MontageSpacing.spacing20
    MontageScreen(modifier = modifier, topBar = { MontageTopBar(title = stringResource(Res.string.home_title)) }) {
        LazyColumn(Modifier.fillMaxWidth()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = gutter, vertical = MontageSpacing.spacing16),
                    verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
                ) {
                    MontageButton(
                        text = stringResource(if (state.canResume) Res.string.home_resume_exam else Res.string.home_start_exam),
                        onClick = { onExamClick(state.resumeSessionId) },
                        loading = state.loading,
                        modifier = Modifier.fillMaxWidth().testTag("home-exam"),
                    )
                    MontageButton(
                        text = stringResource(Res.string.home_practice),
                        onClick = onPracticeClick,
                        variant = MontageButtonVariant.Outlined,
                        type = MontageButtonType.Assistive,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Text(
                    text = stringResource(Res.string.home_past_sessions),
                    style = MontageTheme.typography.label1.bold,
                    color = MontageTheme.colors.foregroundNeutralSecondary,
                    modifier = Modifier.padding(start = gutter, end = gutter, top = MontageSpacing.spacing16, bottom = MontageSpacing.spacing4),
                )
            }
            // 로딩(null) 중에는 빈 안내를 잠깐 보였다 지우지 않도록 아무것도 그리지 않는다.
            if (state.pastSessions?.isEmpty() == true) {
                item {
                    Text(
                        text = stringResource(Res.string.home_past_sessions_empty),
                        style = MontageTheme.typography.body2.regular,
                        color = MontageTheme.colors.foregroundNeutralTertiary,
                        modifier = Modifier.padding(horizontal = gutter, vertical = MontageSpacing.spacing12),
                    )
                }
            }
            items(state.pastSessions.orEmpty(), key = { it.id }) { session ->
                val grade = session.gradeRange ?: stringResource(Res.string.home_grade_pending)
                MontageListCell(
                    title = stringResource(Res.string.home_session_number, session.number),
                    caption = if (session.timeUp) "$grade · ${stringResource(Res.string.home_time_up)}" else grade,
                    chevron = true,
                    onClick = { onSessionClick(session.id) },
                )
            }
        }
    }
}

/** 미리보기와 스크린샷 테스트가 함께 쓰는 고정 상태. */
internal object HomeSamples {
    val loading = HomeUiState()
    val empty = HomeUiState(pastSessions = emptyList())
    val history = HomeUiState(
        resumeSessionId = "d",
        pastSessions = listOf(
            PastSession(id = "c", number = 3, gradeRange = null, timeUp = false),
            PastSession(id = "b", number = 2, gradeRange = "IM2~IH", timeUp = true),
            PastSession(id = "a", number = 1, gradeRange = "IM1", timeUp = false),
        ),
    )
}

@Preview
@Composable
private fun HomeScreenPreview() {
    MontageTheme { HomeScreen(state = HomeSamples.history, onExamClick = {}, onPracticeClick = {}, onSessionClick = {}) }
}

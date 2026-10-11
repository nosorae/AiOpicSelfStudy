package io.github.nosorae.aiopic.ui.featurehome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageStepTrail
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import org.jetbrains.compose.resources.stringResource

/**
 * ViewModel 을 관측하는 바깥 겹. 어디로 갈지는 화면을 등록한 앱 틀이 람다로 정한다.
 * [onExamClick] 은 이어 볼 회차 id 를 받고, 새로 시작하면 null 을 받는다(F3). [onSessionClick] 은 다시 볼 회차 id 를 받는다.
 */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onExamClick: (resumeSessionId: String?) -> Unit,
    onSessionClick: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, onExamClick, onSessionClick, modifier)
}

/**
 * 모의고사 탭(S3). 상태만 받아 그리는 안쪽 겹이고 미리보기와 스크린샷 테스트가 이 함수를 쓴다.
 * 위에서부터 이어 보기(진행 중인 회차가 있을 때), 시작, 다시 보기다. 이어 보기가 있으면 그게 주된 버튼이고 시작은 테두리 버튼으로 물러난다.
 * 회차 목록은 기록 탭이 보이고, 여기서는 가장 최근에 끝낸 회차 하나만 다시 본다.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onExamClick: (resumeSessionId: String?) -> Unit,
    onSessionClick: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    MontageScreen(modifier = modifier, topBar = { MontageTopBar(title = stringResource(Res.string.home_title)) }) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = MontageSpacing.spacing16),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing32),
        ) {
            if (state.canResume) {
                Group {
                    if (state.resumeTotal > 0) {
                        // 처음 보일 때는 움직이지 않는 그림이다. 몇 문항 남았는지를 글을 읽기 전에 보인다.
                        MontageStepTrail(total = state.resumeTotal, done = state.resumeDone.coerceAtMost(state.resumeTotal))
                        Caption(stringResource(Res.string.home_resume_progress, state.resumeTotal, state.resumeDone))
                    }
                    MontageButton(
                        text = stringResource(Res.string.home_resume_exam),
                        onClick = { onExamClick(state.resumeSessionId) },
                        modifier = Modifier.fillMaxWidth().testTag("exam-resume"),
                    )
                }
            }
            // ponytail: 진행 중인 회차를 두고 새로 시작하면 앞 회차는 진행 중으로 남아 목록에 안 보인다. 버릴지 묻는 일이 필요해지면 여기서 확인 대화상자를 띄운다.
            MontageButton(
                text = stringResource(if (state.canResume) Res.string.home_start_new_exam else Res.string.home_start_exam),
                onClick = { onExamClick(null) },
                loading = state.loading,
                variant = if (state.canResume) MontageButtonVariant.Outlined else MontageButtonVariant.Solid,
                type = if (state.canResume) MontageButtonType.Assistive else MontageButtonType.Primary,
                modifier = Modifier.fillMaxWidth().testTag("exam-start"),
            )
            val last = state.pastSessions?.firstOrNull()
            when {
                last != null -> Group {
                    val grade = last.gradeRange ?: stringResource(Res.string.home_grade_pending)
                    val detail = if (last.timeUp) "$grade, ${stringResource(Res.string.home_time_up)}" else grade
                    Caption(stringResource(Res.string.home_review_session, last.number, detail))
                    MontageButton(
                        text = stringResource(Res.string.home_review),
                        onClick = { onSessionClick(last.id) },
                        variant = MontageButtonVariant.Outlined,
                        type = MontageButtonType.Assistive,
                        modifier = Modifier.fillMaxWidth().testTag("exam-review"),
                    )
                }

                // 로딩(null) 중에는 빈 안내를 잠깐 보였다 지우지 않도록 아무것도 그리지 않는다.
                state.pastSessions != null -> Text(
                    text = stringResource(Res.string.home_past_sessions_empty),
                    style = MontageTheme.typography.body2.regular,
                    color = MontageTheme.colors.foregroundNeutralSecondary,
                )
            }
        }
    }
}

/** 설명 한 줄과 그 버튼을 좁게 묶는다. 묶음 사이는 넓게 띄워 어느 설명이 어느 버튼 것인지 보인다. */
@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8), content = content)
}

@Composable
private fun Caption(text: String) {
    Text(text = text, style = MontageTheme.typography.label1.regular, color = MontageTheme.colors.foregroundNeutralSecondary)
}

private val Gutter = MontageSpacing.spacing20

/** 미리보기와 스크린샷 테스트가 함께 쓰는 고정 상태. */
internal object HomeSamples {
    val loading = HomeUiState()
    val empty = HomeUiState(pastSessions = emptyList())
    val finished = HomeUiState(pastSessions = listOf(PastSession(id = "b", number = 2, gradeRange = "IM2~IH", timeUp = true)))
    val history = HomeUiState(
        resumeSessionId = "d",
        resumeDone = 4,
        resumeTotal = 15,
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
    MontageTheme { HomeScreen(state = HomeSamples.history, onExamClick = {}, onSessionClick = {}) }
}

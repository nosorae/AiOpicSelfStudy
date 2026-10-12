package io.github.nosorae.aiopic.ui.featurehome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageButtonType
import io.github.nosorae.aiopic.ui.design.MontageButtonVariant
import io.github.nosorae.aiopic.ui.design.MontageConfirmDialog
import io.github.nosorae.aiopic.ui.design.MontageGatheringDots
import io.github.nosorae.aiopic.ui.design.MontageListCell
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
    val onExam: (String?) -> Unit = { id ->
        // 새로 시작은 이어 볼 회차가 있으면 확인을 받은 뒤에만 여기로 온다. 앞 회차를 미완료로 끝내고 새 회차를 연다.
        if (id == null) viewModel.onIntent(HomeIntent.StartNewExam)
        onExamClick(id)
    }
    HomeScreen(state, onExam, onSessionClick, modifier)
}

/**
 * 모의고사 탭(S3). 상태만 받아 그리는 안쪽 겹이고 미리보기와 스크린샷 테스트가 이 함수를 쓴다.
 * 위에서부터 진행 중인 회차(있을 때), 시험 진행 안내, 시작, 최근 회차다. 기록, 설정 탭과 같은 절 제목과 목록 행으로 그려 탭끼리 같은 앱으로 보인다(#230).
 * 이어 보기가 있으면 그게 주된 버튼이고 시작은 테두리 버튼으로 물러난다.
 * 이어 볼 회차가 있을 때 새로 시작은 앞 회차가 미완료로 끝난다고 묻고 나서 [onExamClick] 에 null 을 넘긴다(#223).
 * 이 화면의 주인공 움직임은 이어 볼 회차의 진행 점이다. 등급을 기다리는 회차 줄의 작은 물결만 기다리는 동안 계속 움직인다.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onExamClick: (resumeSessionId: String?) -> Unit,
    onSessionClick: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingNew by rememberSaveable { mutableStateOf(false) }
    if (confirmingNew) {
        MontageConfirmDialog(
            title = stringResource(Res.string.home_start_new_title),
            message = stringResource(Res.string.home_start_new_message),
            confirm = stringResource(Res.string.home_start_new_exam),
            cancel = stringResource(Res.string.home_start_new_cancel),
            onConfirm = {
                confirmingNew = false
                onExamClick(null)
            },
            onCancel = { confirmingNew = false },
        )
    }
    MontageScreen(modifier = modifier, topBar = { MontageTopBar(title = stringResource(Res.string.home_title)) }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = MontageSpacing.spacing32)) {
            if (state.canResume) {
                SectionTitle(stringResource(Res.string.home_resume_title))
                Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
                    if (state.resumeTotal > 0) {
                        // 처음 보일 때는 움직이지 않는 그림이다. 몇 문항 남았는지를 글을 읽기 전에 보인다.
                        MontageStepTrail(total = state.resumeTotal, done = state.resumeDone.coerceAtMost(state.resumeTotal))
                        Text(
                            text = stringResource(Res.string.home_resume_progress, state.resumeTotal, state.resumeDone),
                            style = MontageTheme.typography.label1.regular,
                            color = MontageTheme.colors.foregroundNeutralSecondary,
                        )
                    }
                    MontageButton(
                        text = stringResource(Res.string.home_resume_exam),
                        onClick = { onExamClick(state.resumeSessionId) },
                        modifier = Modifier.fillMaxWidth().testTag("exam-resume"),
                    )
                }
            }
            state.overview?.let { Overview(it) }
            MontageButton(
                text = stringResource(if (state.canResume) Res.string.home_start_new_exam else Res.string.home_start_exam),
                onClick = { if (state.canResume) confirmingNew = true else onExamClick(null) },
                loading = state.loading,
                variant = if (state.canResume) MontageButtonVariant.Outlined else MontageButtonVariant.Solid,
                type = if (state.canResume) MontageButtonType.Assistive else MontageButtonType.Primary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = MontageSpacing.spacing16).testTag("exam-start"),
            )
            // 로딩(null) 중에는 빈 안내를 잠깐 보였다 지우지 않도록 아무것도 그리지 않는다.
            val past = state.pastSessions ?: return@Column
            SectionTitle(stringResource(Res.string.home_recent_title))
            if (past.isEmpty()) {
                Text(
                    text = stringResource(Res.string.home_past_sessions_empty),
                    style = MontageTheme.typography.body1Reading.regular,
                    color = MontageTheme.colors.foregroundNeutralSecondary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = MontageSpacing.spacing4),
                )
            }
            // ponytail: 여기는 최근 셋만 보인다. 전체 목록과 거르기는 기록 탭이 맡는다.
            past.take(RECENT).forEach { session -> key(session.id) { SessionRow(session, onClick = { onSessionClick(session.id) }) } }
        }
    }
}

/** 시작 전에 시험이 어떻게 흘러가는지 단계 셋으로 보인다. 누르는 행이 아니라서 화살표가 없다. */
@Composable
private fun Overview(overview: ExamOverview) {
    SectionTitle(stringResource(Res.string.home_overview_title))
    if (overview.minTopics > 0) {
        MontageListCell(
            title = stringResource(Res.string.home_overview_survey),
            caption = stringResource(Res.string.home_overview_survey_caption, overview.minTopics),
        )
    }
    MontageListCell(
        title = stringResource(Res.string.home_overview_level),
        caption = stringResource(Res.string.home_overview_level_caption, overview.levelCount),
    )
    MontageListCell(
        title = if (overview.minQuestions == overview.maxQuestions) {
            stringResource(Res.string.home_overview_questions_fixed, overview.maxQuestions)
        } else {
            stringResource(Res.string.home_overview_questions, overview.minQuestions, overview.maxQuestions)
        },
        caption = overview.recalibrateAfter?.let { stringResource(Res.string.home_overview_questions_recalibrate, overview.minutes, it) }
            ?: stringResource(Res.string.home_overview_questions_caption, overview.minutes),
    )
}

/** 기록 탭의 회차 줄과 같은 모양이다. 등급이 나왔으면 오른쪽에 크게, 기다리는 중이면 작은 물결과 무엇이 나오는지를 보인다. */
@Composable
private fun SessionRow(session: PastSession, onClick: () -> Unit) {
    val pending = session.gradeRange == null
    MontageListCell(
        title = stringResource(Res.string.home_session, session.number),
        caption = when {
            pending -> stringResource(Res.string.home_grade_pending)
            session.timeUp -> stringResource(Res.string.home_time_up)
            else -> null
        },
        chevron = true,
        onClick = onClick,
        modifier = Modifier.testTag("exam-review"),
    ) {
        if (pending) {
            MontageGatheringDots(stringResource(Res.string.home_grade_pending), Modifier.size(width = MontageSpacing.spacing48, height = MontageSpacing.spacing24))
        } else {
            Text(session.gradeRange.orEmpty(), style = MontageTheme.typography.headline2.bold, color = MontageTheme.colors.foregroundNeutralStrong)
        }
    }
}

/** 기록, 설정 탭과 같은 절 제목. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MontageTheme.typography.label1.bold,
        color = MontageTheme.colors.foregroundNeutralSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Gutter, end = Gutter, top = MontageSpacing.spacing24, bottom = MontageSpacing.spacing8)
            .semantics { heading() },
    )
}

private val Gutter = MontageSpacing.spacing20

private const val RECENT = 3

/** 미리보기와 스크린샷 테스트가 함께 쓰는 고정 상태. */
internal object HomeSamples {
    private val overview = ExamOverview(minTopics = 12, levelCount = 6, minQuestions = 12, maxQuestions = 15, minutes = 40, recalibrateAfter = 7)
    val loading = HomeUiState()
    val empty = HomeUiState(pastSessions = emptyList(), overview = overview)
    val finished = HomeUiState(
        pastSessions = listOf(
            PastSession(id = "b", number = 2, gradeRange = "IM2~IH", timeUp = true),
            PastSession(id = "a", number = 1, gradeRange = "IM1", timeUp = false),
        ),
        overview = overview,
    )
    val history = HomeUiState(
        resumeSessionId = "d",
        resumeDone = 4,
        resumeTotal = 15,
        pastSessions = listOf(
            PastSession(id = "c", number = 4, gradeRange = null, timeUp = false),
            PastSession(id = "b", number = 3, gradeRange = "IM2~IH", timeUp = true),
            PastSession(id = "a", number = 2, gradeRange = "IM1", timeUp = false),
            PastSession(id = "z", number = 1, gradeRange = "IL", timeUp = false),
        ),
        overview = overview,
    )
}

@Preview
@Composable
private fun HomeScreenPreview() {
    MontageTheme { HomeScreen(state = HomeSamples.history, onExamClick = {}, onSessionClick = {}) }
}

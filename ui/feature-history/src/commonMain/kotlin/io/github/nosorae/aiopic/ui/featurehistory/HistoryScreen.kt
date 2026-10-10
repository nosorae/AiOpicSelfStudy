package io.github.nosorae.aiopic.ui.featurehistory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import io.github.nosorae.aiopic.ui.design.MontageGradeScale
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.design.MontageProgressBar
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import org.jetbrains.compose.resources.stringResource

@Composable
fun HistoryRoute(viewModel: HistoryViewModel, onOpenSession: (sessionId: String) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state = state, onOpenSession = onOpenSession, modifier = modifier)
}

/**
 * 기록 탭(S13, F7.3). 회차마다 예상 등급과 분석 모델을 저장한 순서대로 보이고, 모델이 섞인 회차는 표시한다(F5.7).
 * 아래에 문항 유형별 약점 점수를 약한 것부터 막대와 함께 둔다. 회차를 누르면 [onOpenSession] 로 회차 리포트를 연다.
 * 이 화면의 움직임은 하나다. 맨 위 등급 눈금에서 최근 회차 등급으로 구슬이 미끄러져 멈춘다. 약점 막대는 처음부터 그 길이다.
 */
@Composable
fun HistoryScreen(state: HistoryUiState, onOpenSession: (sessionId: String) -> Unit, modifier: Modifier = Modifier) {
    MontageScreen(modifier = modifier, topBar = { MontageTopBar(title = stringResource(Res.string.history_title)) }) {
        when {
            state.loading -> Unit

            state.empty -> Body(stringResource(Res.string.history_empty))

            else -> LazyColumn(Modifier.fillMaxWidth()) {
                if (state.sessions.isNotEmpty()) {
                    item {
                        SectionTitle(stringResource(Res.string.history_grades))
                        LatestGrade(state)
                    }
                }
                itemsIndexed(state.sessions, key = { _, row -> row.session.id }) { index, row ->
                    val models = row.models.joinToString(", ")
                    MontageListCell(
                        title = stringResource(Res.string.history_session, index + 1),
                        caption = when {
                            row.models.isEmpty() -> stringResource(Res.string.history_models_none)
                            row.mixedModels -> stringResource(Res.string.history_models_mixed, models)
                            else -> stringResource(Res.string.history_models, models)
                        },
                        chevron = true,
                        onClick = { onOpenSession(row.session.id) },
                    ) {
                        val grade = row.session.grade
                        Text(
                            text = when {
                                grade != null -> grade.range
                                row.answered > 0 -> stringResource(Res.string.history_grade_waiting, row.analyzed, row.answered)
                                else -> stringResource(Res.string.history_grade_pending)
                            },
                            style = if (grade == null) MontageTheme.typography.label1.regular else MontageTheme.typography.headline2.bold,
                            color = if (grade == null) MontageTheme.colors.foregroundNeutralSecondary else MontageTheme.colors.foregroundNeutralStrong,
                        )
                    }
                }
                if (state.weaknesses.isNotEmpty()) {
                    item {
                        SectionTitle(stringResource(Res.string.history_weakness))
                        Body(stringResource(Res.string.history_weakness_help))
                    }
                }
                items(state.weaknesses, key = { it.typeName }) { weakness ->
                    MontageListCell(title = weakness.typeName) {
                        MontageProgressBar(weakness.score / 100f, Modifier.width(96.dp).height(4.dp).align(Alignment.CenterVertically))
                        Text(
                            text = stringResource(Res.string.history_score, weakness.score),
                            style = MontageTheme.typography.body2.bold,
                            color = MontageTheme.colors.foregroundNeutralPrimary,
                        )
                    }
                }
            }
        }
    }
}

/** 등급이 나온 가장 최근 회차를 등급 눈금에 놓는다. 등급 이름이 시험 팩 눈금에 없으면 그리지 않는다. */
@Composable
private fun LatestGrade(state: HistoryUiState) {
    val grade = state.sessions.lastOrNull { it.session.grade != null }?.session?.grade ?: return
    val names = state.grades
    if (grade.low !in names || grade.high !in names) return
    val label = stringResource(Res.string.history_latest, grade.range)
    Column {
        MontageGradeScale(
            names,
            names.indexOf(grade.low),
            names.indexOf(grade.high),
            description = label,
            modifier = Modifier.padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing8),
        )
        Body(label)
    }
}

/** 회차 리포트와 같은 표기. 체크 결과가 한 등급으로 모이면 하나만 쓴다. */
private val SessionGrade.range: String get() = if (low == high) low else "$low ~ $high"

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MontageTheme.typography.label1.bold,
        color = MontageTheme.colors.foregroundNeutralSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MontageSpacing.spacing20, end = MontageSpacing.spacing20, top = MontageSpacing.spacing24, bottom = MontageSpacing.spacing8)
            .semantics { heading() },
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MontageTheme.typography.body1Reading.regular,
        color = MontageTheme.colors.foregroundNeutralSecondary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing4),
    )
}

@Preview
@Composable
private fun HistoryScreenPreview() {
    MontageTheme { HistoryScreen(state = HistorySamples.filled, onOpenSession = {}) }
}

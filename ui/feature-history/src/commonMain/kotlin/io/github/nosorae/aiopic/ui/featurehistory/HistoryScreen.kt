package io.github.nosorae.aiopic.ui.featurehistory

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import io.github.nosorae.aiopic.ui.design.MontageListCell
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
 * 아래에 문항 유형별 약점 점수를 약한 것부터 둔다. 회차를 누르면 [onOpenSession] 로 회차 리포트를 연다.
 */
@Composable
fun HistoryScreen(state: HistoryUiState, onOpenSession: (sessionId: String) -> Unit, modifier: Modifier = Modifier) {
    MontageScreen(modifier = modifier, topBar = { MontageTopBar(title = stringResource(Res.string.history_title)) }) {
        when {
            state.loading -> Unit

            state.empty -> Body(stringResource(Res.string.history_empty))

            else -> LazyColumn(Modifier.fillMaxWidth()) {
                if (state.sessions.isNotEmpty()) item { SectionTitle(stringResource(Res.string.history_grades)) }
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
                            text = grade?.range ?: stringResource(Res.string.history_grade_pending),
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

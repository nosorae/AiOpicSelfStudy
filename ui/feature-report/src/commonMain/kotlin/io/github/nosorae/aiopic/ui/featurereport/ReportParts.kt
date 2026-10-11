package io.github.nosorae.aiopic.ui.featurereport

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageButtonVariant
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** 리포트 두 화면이 같은 모양으로 쓰는 절 제목. 화면 읽기 프로그램이 절 단위로 건너뛸 수 있게 제목으로 표시한다. */
@Composable
internal fun SectionTitle(text: String) {
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
internal fun Paragraph(text: String, secondary: Boolean = false) {
    Text(
        text = text,
        style = MontageTheme.typography.body1Reading.regular,
        color = if (secondary) MontageTheme.colors.foregroundNeutralSecondary else MontageTheme.colors.foregroundNeutralPrimary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing4),
    )
}

/**
 * 리포트에 보일 문항 유형 이름과 분석 모델 이름. 유형 이름은 시험 팩이 정해서 앱 층이 넣고, 모델은 앱이 들고 있는 목록의 표시 이름을 쓴다(F5.7).
 * 이름을 모르는 유형과 목록에서 빠진 모델은 id 를 그대로 보인다. [grades] 는 등급 눈금에 늘어놓을 시험 팩의 등급 이름(낮은 것부터)이다.
 */
data class ReportNames(val types: Map<QuestionType, String> = emptyMap(), val models: List<ModelInfo> = emptyList(), val grades: List<String> = emptyList()) {
    fun type(type: QuestionType): String = types[type] ?: type.id

    fun model(by: AnalyzedBy): String = models.firstOrNull { it.provider == by.provider && it.id == by.modelId }?.displayName ?: by.modelId
}

internal val UnscorableReason.text: StringResource
    get() = when (this) {
        UnscorableReason.Silent -> Res.string.reason_silent
        UnscorableReason.WrongLanguage -> Res.string.reason_wrong_language
        UnscorableReason.TooShort -> Res.string.reason_too_short
    }

/** 분석하지 못한 이유와 사용자가 할 일(#203). */
internal val AnalysisError.text: StringResource
    get() = when (this) {
        AnalysisError.Retryable -> Res.string.failed_retryable
        AnalysisError.InvalidKey -> Res.string.failed_invalid_key
        AnalysisError.ModelUnavailable -> Res.string.failed_model_unavailable
        AnalysisError.Rejected -> Res.string.failed_rejected
    }

/** 리포트 두 화면의 다시 분석하기. 누르면 바로 기다림으로 바뀌어 처리 중 표시는 두지 않는다. */
@Composable
internal fun RetryButton(onClick: () -> Unit, tag: String) {
    MontageButton(
        text = stringResource(Res.string.retry_analysis),
        onClick = onClick,
        variant = MontageButtonVariant.Outlined,
        modifier = Modifier.padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing8).testTag(tag),
    )
}

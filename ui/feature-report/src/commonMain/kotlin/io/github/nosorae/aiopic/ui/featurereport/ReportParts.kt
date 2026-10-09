package io.github.nosorae.aiopic.ui.featurereport

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.jetbrains.compose.resources.StringResource

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

/** 리포트와 기록에 보이는 모델 이름. 제공사와 모델 id 를 그대로 보여 준다(F5.7). */
internal val AnalyzedBy.label: String get() = "$provider $modelId"

internal val UnscorableReason.text: StringResource
    get() = when (this) {
        UnscorableReason.Silent -> Res.string.reason_silent
        UnscorableReason.WrongLanguage -> Res.string.reason_wrong_language
        UnscorableReason.TooShort -> Res.string.reason_too_short
    }

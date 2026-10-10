package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.nosorae.aiopic.ui.design.MontageGatheringDots
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.jetbrains.compose.resources.stringResource

/**
 * 질문을 기다리는 동안 문항 화면의 진행 문구 대신 보인다. 점 무리가 움직여 기다리는 중임을 보이고(동작 줄이기면 멈춘 물결),
 * [text] 가 무엇을 만드는지 또는 오래 걸리면 연결 상태를 보라고 알린다.
 */
@Composable
internal fun QuestionLoading(text: String, modifier: Modifier = Modifier) {
    Column(modifier.testTag("exam-question-loading"), verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12)) {
        MontageGatheringDots(stringResource(Res.string.exam_question_loading_description))
        Text(text = text, style = MontageTheme.typography.heading2.bold, color = MontageTheme.colors.foregroundNeutralPrimary)
    }
}

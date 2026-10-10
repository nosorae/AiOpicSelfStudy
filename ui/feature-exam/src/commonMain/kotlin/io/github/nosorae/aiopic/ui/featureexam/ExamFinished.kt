package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.nosorae.aiopic.ui.design.MontageCelebration
import io.github.nosorae.aiopic.ui.design.MontageCelebrationSize
import io.github.nosorae.aiopic.ui.design.MontageCheckMark
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.jetbrains.compose.resources.stringResource

/**
 * 마지막 문항까지 답한 순간 리포트로 넘어가기 전 잠깐 보이는 화면. 40분을 버틴 끝이라 큰 축하를 한 번 터뜨리고 축하 진동을 낸다.
 * 체크와 문구가 결과를 전하고 축하 조각은 꾸밈이다. 동작 줄이기면 조각 없이 체크와 문구, 축하 진동만 남는다.
 * 머무는 2초 동안 화면을 돌려 다시 그려도 [sessionId] 키로 축하와 진동은 한 번만 난다.
 */
@Composable
internal fun ExamFinished(questionCount: Int, sessionId: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(MontageTheme.colors.backgroundNeutralPrimary).padding(horizontal = MontageSpacing.spacing24),
        verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 조각은 체크 가운데에서 체크보다 넓게 터지되 자리를 차지하지 않아 문구가 체크 바로 밑에 붙는다.
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            MontageCelebration(MontageCelebrationSize.Large, Modifier.requiredSize(280.dp), playOnceKey = "exam-finished:$sessionId")
            MontageCheckMark(Modifier.fillMaxSize())
        }
        Text(
            text = stringResource(Res.string.exam_finished_title, questionCount),
            style = MontageTheme.typography.title2.bold,
            color = MontageTheme.colors.foregroundNeutralStrong,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            text = stringResource(Res.string.exam_finished_next),
            style = MontageTheme.typography.body1Reading.regular,
            color = MontageTheme.colors.foregroundNeutralSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun ExamFinishedPreview() {
    MontageTheme { ExamFinished(questionCount = 15, sessionId = "preview") }
}

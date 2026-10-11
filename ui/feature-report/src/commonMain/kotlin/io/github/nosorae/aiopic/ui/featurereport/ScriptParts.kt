package io.github.nosorae.aiopic.ui.featurereport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.nosorae.aiopic.engine.answer.model.ContentFix
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import io.github.nosorae.aiopic.ui.design.LocalHaptics
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageButtonSize
import io.github.nosorae.aiopic.ui.design.MontageButtonVariant
import io.github.nosorae.aiopic.ui.design.MontageCheckMark
import io.github.nosorae.aiopic.ui.design.MontageRadius
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.jetbrains.compose.resources.stringResource

/**
 * 내용 교정 하나(#217). 무엇이 어색한지를 먼저 굵게 쓰고 원문, 고친 문장을 잇는다.
 * 표현 교정은 고친 문장이 주인공이지만 여기서는 왜 어색한지가 처음 보는 정보라서 순서를 바꿨다.
 */
@Composable
internal fun ContentFixRow(fix: ContentFix) {
    val colors = MontageTheme.colors
    val typography = MontageTheme.typography
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing12),
        verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing4),
    ) {
        Text(fix.problem, style = typography.body2.bold, color = colors.foregroundNeutralStrong)
        Text(stringResource(Res.string.answer_original, fix.original), style = typography.label1.regular, color = colors.foregroundNeutralSecondary)
        Text(stringResource(Res.string.answer_corrected, fix.corrected), style = typography.body2.regular, color = colors.foregroundNeutralPrimary)
    }
}

/**
 * 최종 스크립트(#217). 따라 읽을 글이라 옅은 면 위에 본문 크기로 두고, 아래에 복사와 듣기를 둔다.
 * 듣기는 자연스러운 음성을 받는 몇 초 동안 버튼에 처리 중 표시를 하고, 읽는 동안은 멈추기로 바뀐다.
 * 복사는 바로 끝나서 체크와 문구, 성공 진동으로만 알린다. 진동은 누를 때 한 번 내고, 체크는 [answerId] 로 한 번만 그린다.
 * [copied] 는 계속 true 로 남아서, 그릴 때 알리면 스크롤로 다시 내려오거나 화면을 돌릴 때마다 또 울린다.
 */
@Composable
internal fun FinalScript(
    answerId: String,
    script: String,
    playback: ScriptPlayback,
    copied: Boolean,
    onCopyClick: () -> Unit,
    onListenClick: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val colors = MontageTheme.colors
    val typography = MontageTheme.typography
    Column(Modifier.fillMaxWidth().padding(horizontal = MontageSpacing.spacing20), verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12)) {
        Text(stringResource(Res.string.answer_script_help), style = typography.label1.regular, color = colors.foregroundNeutralSecondary)
        Text(
            text = script,
            style = typography.body1Reading.regular,
            color = colors.foregroundNeutralStrong,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.backgroundNeutralSecondary, RoundedCornerShape(MontageRadius.radius16))
                .padding(MontageSpacing.spacing16)
                .testTag("final-script"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
            MontageButton(
                text = stringResource(if (playback == ScriptPlayback.Playing) Res.string.answer_script_stop else Res.string.answer_script_listen),
                onClick = onListenClick,
                size = MontageButtonSize.Medium,
                loading = playback == ScriptPlayback.Preparing,
                modifier = Modifier.weight(1f).testTag("script-listen"),
            )
            MontageButton(
                text = stringResource(Res.string.answer_script_copy),
                onClick = {
                    haptics.play(HapticEffect.Success)
                    onCopyClick()
                },
                variant = MontageButtonVariant.Outlined,
                size = MontageButtonSize.Medium,
                modifier = Modifier.weight(1f).testTag("script-copy"),
            )
        }
        if (copied) {
            Row(horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing4), verticalAlignment = Alignment.CenterVertically) {
                MontageCheckMark(Modifier.size(MontageSpacing.spacing40), playOnceKey = "script-copied:$answerId")
                Text(
                    text = stringResource(Res.string.answer_script_copied),
                    style = typography.body2.regular,
                    color = colors.foregroundNeutralPrimary,
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

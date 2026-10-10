package io.github.nosorae.aiopic.ui.featurereport.flag

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import io.github.nosorae.aiopic.ui.design.LocalHaptics
import io.github.nosorae.aiopic.ui.design.MontageChip
import io.github.nosorae.aiopic.ui.design.MontageChipVariant
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.featurereport.Res
import io.github.nosorae.aiopic.ui.featurereport.answer_corrected
import io.github.nosorae.aiopic.ui.featurereport.answer_original
import io.github.nosorae.aiopic.ui.featurereport.flag_wrong
import org.jetbrains.compose.resources.stringResource

/** 교정 한 줄과 "틀림" 칩(F8.1). 틀림으로 표시하면 고친 문장에 주홍 줄이 그어지고 선택 진동을 낸다. 칩을 다시 누르면 표시가 풀린다. */
@Composable
internal fun CorrectionRow(correction: Correction, wrong: Boolean, onWrongClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHaptics.current
    MontageListCell(
        title = stringResource(Res.string.answer_corrected, correction.corrected),
        caption = stringResource(Res.string.answer_original, correction.original),
        modifier = modifier,
        struck = wrong,
    ) {
        MontageChip(
            text = stringResource(Res.string.flag_wrong),
            onClick = {
                haptics.play(HapticEffect.Selection)
                onWrongClick()
            },
            active = wrong,
            variant = MontageChipVariant.Outlined,
            modifier = Modifier.semantics { toggleableState = ToggleableState(wrong) },
        )
    }
}

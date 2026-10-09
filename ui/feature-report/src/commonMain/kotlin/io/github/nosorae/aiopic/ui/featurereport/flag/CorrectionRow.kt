package io.github.nosorae.aiopic.ui.featurereport.flag

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.ui.design.MontageChip
import io.github.nosorae.aiopic.ui.design.MontageChipVariant
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.featurereport.Res
import io.github.nosorae.aiopic.ui.featurereport.answer_corrected
import io.github.nosorae.aiopic.ui.featurereport.answer_original
import io.github.nosorae.aiopic.ui.featurereport.flag_wrong
import org.jetbrains.compose.resources.stringResource

/** 교정 한 줄과 "틀림" 칩(F8.1). 칩을 다시 누르면 표시가 풀린다. */
@Composable
internal fun CorrectionRow(correction: Correction, wrong: Boolean, onWrongClick: () -> Unit, modifier: Modifier = Modifier) {
    MontageListCell(
        title = stringResource(Res.string.answer_corrected, correction.corrected),
        caption = stringResource(Res.string.answer_original, correction.original),
        modifier = modifier,
    ) {
        MontageChip(
            text = stringResource(Res.string.flag_wrong),
            onClick = onWrongClick,
            active = wrong,
            variant = MontageChipVariant.Outlined,
            modifier = Modifier.semantics { toggleableState = ToggleableState(wrong) },
        )
    }
}

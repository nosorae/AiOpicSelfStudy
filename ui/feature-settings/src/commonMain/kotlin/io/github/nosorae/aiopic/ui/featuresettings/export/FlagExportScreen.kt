package io.github.nosorae.aiopic.ui.featuresettings.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import io.github.nosorae.aiopic.ui.featuresettings.Res
import io.github.nosorae.aiopic.ui.featuresettings.export_copied
import io.github.nosorae.aiopic.ui.featuresettings.export_copy
import io.github.nosorae.aiopic.ui.featuresettings.export_count
import io.github.nosorae.aiopic.ui.featuresettings.export_empty
import io.github.nosorae.aiopic.ui.featuresettings.export_help
import io.github.nosorae.aiopic.ui.featuresettings.export_title
import org.jetbrains.compose.resources.stringResource

/** 복사 버튼을 누르면 JSON 을 클립보드에 넣는다. 서버가 없어서 사용자가 이슈에 직접 붙인다(F8.3). */
@Composable
fun FlagExportRoute(viewModel: FlagExportViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // ponytail: 공유 시트, 파일 저장 대신 클립보드 하나로 두 플랫폼을 같은 코드로 덮는다. 새 클립보드 API 는 넣는 값을
    // 플랫폼마다 따로 만들어야 해서 예전 API 를 쓴다. 이슈 본문 한도(65,536자)를 넘길 만큼 쌓이면 파일 공유로 바꾼다.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var copied by rememberSaveable { mutableStateOf(false) }
    FlagExportScreen(
        state = state,
        copied = copied,
        onCopyClick = {
            clipboard.setText(AnnotatedString(state.json))
            copied = true
        },
        modifier = modifier,
    )
}

/** 표시 수, 무엇이 들어가는지, 복사 버튼. 표시가 없으면 어디서 표시하는지 알려 주고 버튼을 끈다. */
@Composable
fun FlagExportScreen(state: FlagExportUiState, copied: Boolean, onCopyClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MontageTheme.colors
    val typography = MontageTheme.typography
    MontageScreen(
        modifier = modifier,
        topBar = { MontageTopBar(title = stringResource(Res.string.export_title)) },
        bottomBar = {
            MontageButton(
                text = stringResource(Res.string.export_copy),
                onClick = onCopyClick,
                enabled = state.count > 0,
                modifier = Modifier.fillMaxWidth().padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing12),
            )
        },
    ) {
        if (state.loading) return@MontageScreen
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing16),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12),
        ) {
            Text(stringResource(Res.string.export_count, state.count), style = typography.title3.bold, color = colors.foregroundNeutralStrong)
            Text(
                text = stringResource(if (state.count > 0) Res.string.export_help else Res.string.export_empty),
                style = typography.body1Reading.regular,
                color = colors.foregroundNeutralSecondary,
            )
            if (copied) {
                Text(
                    text = stringResource(Res.string.export_copied),
                    style = typography.body1Reading.regular,
                    color = colors.foregroundNeutralPrimary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

/** 미리보기와 스크린샷 테스트가 같이 쓰는 고정 상태. */
internal object FlagExportSamples {
    val flagged = FlagExportUiState(loading = false, count = 3, json = "[]")
    val empty = FlagExportUiState(loading = false)
}

@Preview
@Composable
private fun FlagExportScreenPreview() {
    MontageTheme { FlagExportScreen(state = FlagExportSamples.flagged, copied = true, onCopyClick = {}) }
}

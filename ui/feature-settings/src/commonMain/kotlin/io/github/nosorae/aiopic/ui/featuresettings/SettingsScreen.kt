package io.github.nosorae.aiopic.ui.featuresettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.ui.design.MontageAlert
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageButtonSize
import io.github.nosorae.aiopic.ui.design.MontageButtonType
import io.github.nosorae.aiopic.ui.design.MontageButtonVariant
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider
import io.github.nosorae.aiopic.ui.featuresettings.key.KeyStatus
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * ViewModel 을 관측하는 바깥 겹. 키 입력과 무료 등급 안내(동의 화면)는 단순 이동이라 화면을 등록한 쪽이 람다로 연다.
 * 키 입력에서 돌아오면 화면이 다시 보일 때마다 키 저장 여부를 새로 읽는다.
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onOpenKeyEntry: (KeyProvider) -> Unit,
    onOpenFreeTierNotice: () -> Unit,
    onOpenFlagExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(SettingsIntent.Reload) }
    SettingsScreen(state, viewModel::onIntent, onOpenKeyEntry, onOpenFreeTierNotice, onOpenFlagExport, modifier)
}

/** 상태만 받아 그리는 안쪽 겹. 미리보기와 스크린샷 테스트가 이 함수를 쓴다. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onOpenKeyEntry: (KeyProvider) -> Unit,
    onOpenFreeTierNotice: () -> Unit,
    onOpenFlagExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MontageScreen(modifier = modifier, topBar = { MontageTopBar(title = stringResource(Res.string.settings_title)) }) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = MontageSpacing.spacing16)) {
            SectionLabel(Res.string.settings_model_section)
            state.fellBackFrom?.let { removed ->
                Notice(stringResource(Res.string.settings_model_fell_back, removed, state.selectedModel?.displayName.orEmpty()))
            }
            state.models.forEach { model ->
                MontageListCell(
                    title = model.displayName,
                    caption = modelCaption(model),
                    selected = model.id == state.selectedModelId,
                    onClick = { onIntent(SettingsIntent.SelectModel(model.id)) },
                )
            }
            if (state.showsPreviewNotice) Notice(stringResource(Res.string.settings_model_preview_notice))
            MontageButton(
                text = stringResource(Res.string.settings_free_tier_link),
                onClick = onOpenFreeTierNotice,
                variant = MontageButtonVariant.Text,
                size = MontageButtonSize.Medium,
                // 글자 버튼의 안쪽 여백에 12dp 를 더해 글자를 다른 줄의 20dp 에 거의 맞춘다.
                modifier = Modifier.padding(horizontal = MontageSpacing.spacing12),
            )

            SectionLabel(Res.string.settings_key_section)
            state.keys.forEach { key ->
                KeyRow(key, onOpen = { onOpenKeyEntry(key.provider) }, onDelete = { onIntent(SettingsIntent.RequestKeyDeletion(key.provider)) })
            }

            SectionLabel(Res.string.settings_theme_section)
            ThemeMode.entries.forEach { mode ->
                MontageListCell(
                    title = stringResource(themeLabel(mode)),
                    selected = mode == state.themeMode,
                    onClick = { onIntent(SettingsIntent.SelectThemeMode(mode)) },
                )
            }

            val uriHandler = LocalUriHandler.current
            MontageListCell(
                title = stringResource(Res.string.settings_privacy_policy),
                // 화면 모드 선택지로 읽히지 않게 묶음 사이 간격만큼 띄운다.
                modifier = Modifier.padding(top = MontageSpacing.spacing24),
                chevron = true,
                onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
            )
            // 리포트에서 "틀림"을 누른 교정을 JSON 으로 복사하는 화면(F8.3).
            MontageListCell(title = stringResource(Res.string.export_title), chevron = true, onClick = onOpenFlagExport)
        }
    }
    state.deletingKey?.let { provider ->
        MontageAlert(
            title = stringResource(Res.string.settings_key_delete_title, provider.label),
            message = stringResource(Res.string.settings_key_delete_message),
            confirm = stringResource(Res.string.settings_key_delete),
            onConfirm = { onIntent(SettingsIntent.DeleteKey) },
            cancel = stringResource(Res.string.settings_key_delete_cancel),
            onDismissRequest = { onIntent(SettingsIntent.CancelKeyDeletion) },
            negative = true,
        )
    }
}

/** 서버가 없어 방침 문서는 저장소에 둔다. 스토어 등록 화면에도 같은 주소를 넣는다. */
private const val PRIVACY_POLICY_URL = "https://github.com/nosorae/AiOpicSelfStudy/blob/main/docs/privacy-policy.md"

@Composable
private fun SectionLabel(text: StringResource) {
    Text(
        text = stringResource(text),
        style = MontageTheme.typography.label1.bold,
        color = MontageTheme.colors.foregroundNeutralSecondary,
        modifier = Modifier.padding(
            start = MontageSpacing.spacing20,
            end = MontageSpacing.spacing20,
            top = MontageSpacing.spacing24,
            bottom = MontageSpacing.spacing8,
        ),
    )
}

@Composable
private fun Notice(text: String) {
    Text(
        text = text,
        style = MontageTheme.typography.label1.regular,
        color = MontageTheme.colors.foregroundNeutralSecondary,
        modifier = Modifier.padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing8),
    )
}

@Composable
private fun KeyRow(key: KeyStatus, onOpen: () -> Unit, onDelete: () -> Unit) {
    MontageListCell(
        title = key.provider.label,
        caption = stringResource(if (key.saved) Res.string.settings_key_saved else Res.string.settings_key_missing),
        chevron = !key.saved,
        onClick = onOpen,
    ) {
        if (key.saved) {
            MontageButton(
                text = stringResource(Res.string.settings_key_delete),
                onClick = onDelete,
                variant = MontageButtonVariant.Text,
                type = MontageButtonType.Assistive,
                size = MontageButtonSize.Medium,
            )
        }
    }
}

@Composable
private fun modelCaption(model: ModelInfo): String = stringResource(
    Res.string.settings_model_caption,
    model.id,
    stringResource(if (model.hearsAudio) Res.string.settings_model_hears_audio else Res.string.settings_model_transcribes),
    stringResource(if (model.stability == Stability.Stable) Res.string.settings_model_stable else Res.string.settings_model_preview),
)

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.System -> Res.string.settings_theme_system
    ThemeMode.Light -> Res.string.settings_theme_light
    ThemeMode.Dark -> Res.string.settings_theme_dark
}

/** 미리보기와 스크린샷 테스트가 함께 쓰는 고정 상태. */
internal object SettingsSamples {
    private val stable = ModelInfo("gemini", "gemini-3.8-flash", "Gemini 3.8 Flash", hearsAudio = true, stability = Stability.Stable)
    private val preview = ModelInfo("gemini", "gemini-4-pro-preview", "Gemini 4 Pro Preview", hearsAudio = true, stability = Stability.Preview)

    val default = SettingsUiState(
        models = listOf(stable, preview),
        selectedModelId = stable.id,
        keys = listOf(KeyStatus(KeyProvider.Gemini, saved = true)),
    )
    val previewSelected = default.copy(selectedModelId = preview.id)
    val fellBack = default.copy(fellBackFrom = "gemini-2.0-flash", keys = listOf(KeyStatus(KeyProvider.Gemini, saved = false)))
    val dark = default.copy(themeMode = ThemeMode.Dark)
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    MontageTheme { SettingsScreen(SettingsSamples.previewSelected, onIntent = {}, onOpenKeyEntry = {}, onOpenFreeTierNotice = {}, onOpenFlagExport = {}) }
}

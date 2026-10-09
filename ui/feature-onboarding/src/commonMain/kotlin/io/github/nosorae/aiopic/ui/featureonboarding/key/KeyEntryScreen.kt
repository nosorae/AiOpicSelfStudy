package io.github.nosorae.aiopic.ui.featureonboarding.key

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTextField
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import io.github.nosorae.aiopic.ui.featureonboarding.Res
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_checking
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_error_invalid
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_error_retryable
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_key
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_key_description
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_key_placeholder
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_provider
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_submit
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_title
import org.jetbrains.compose.resources.stringResource

/** ViewModel 을 관측해 이동을 처리하는 바깥 겹. 키 저장 뒤 어디로 갈지는 화면을 등록한 쪽이 [onDone] 으로 정한다. */
@Composable
fun KeyEntryRoute(viewModel: KeyEntryViewModel, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 화면을 다시 그리는 사이 람다가 바뀌어도 이동 처리 중인 효과가 새 람다를 부르게 한다.
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.navigation) {
        if (state.navigation == KeyEntryDestination.KeySaved) {
            currentOnDone()
            viewModel.onIntent(KeyEntryIntent.NavigationHandled)
        }
    }
    KeyEntryScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태만 받아 그리는 안쪽 겹. 미리보기와 스크린샷 테스트가 이 함수를 쓴다. */
@Composable
fun KeyEntryScreen(state: KeyEntryUiState, onIntent: (KeyEntryIntent) -> Unit, modifier: Modifier = Modifier) {
    val gutter = MontageSpacing.spacing20
    MontageScreen(
        modifier = modifier,
        topBar = { MontageTopBar(title = stringResource(Res.string.key_entry_title)) },
        bottomBar = {
            MontageButton(
                text = stringResource(if (state.checking) Res.string.key_entry_checking else Res.string.key_entry_submit),
                onClick = { onIntent(KeyEntryIntent.SubmitKey) },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = MontageSpacing.spacing12),
            )
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = MontageSpacing.spacing16),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
        ) {
            Text(
                text = stringResource(Res.string.key_entry_provider),
                style = MontageTheme.typography.label1.bold,
                color = MontageTheme.colors.foregroundNeutralSecondary,
                modifier = Modifier.padding(horizontal = gutter),
            )
            Column {
                state.providers.forEach { provider ->
                    MontageListCell(
                        title = provider.label,
                        selected = provider == state.selectedProvider,
                        enabled = !state.checking,
                        onClick = { onIntent(KeyEntryIntent.SelectProvider(provider)) },
                    )
                }
            }
            MontageTextField(
                value = state.key,
                onValueChange = { onIntent(KeyEntryIntent.ChangeKey(it)) },
                title = stringResource(Res.string.key_entry_key),
                placeholder = stringResource(Res.string.key_entry_key_placeholder),
                description = stringResource(
                    when (state.error) {
                        KeyCheckResult.Invalid -> Res.string.key_entry_error_invalid
                        KeyCheckResult.Retryable -> Res.string.key_entry_error_retryable
                        KeyCheckResult.Valid, null -> Res.string.key_entry_key_description
                    },
                ),
                error = state.error != null,
                enabled = !state.checking,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.padding(horizontal = gutter),
            )
        }
    }
}

/** 미리보기와 스크린샷 테스트가 함께 쓰는 고정 상태. */
internal object KeyEntrySamples {
    val empty = KeyEntryUiState()
    val ready = KeyEntryUiState(selectedProvider = KeyProvider.Gemini, key = "test-key")
    val checking = ready.copy(checking = true)
    val invalid = ready.copy(error = KeyCheckResult.Invalid)
    val retryable = ready.copy(error = KeyCheckResult.Retryable)
}

@Preview
@Composable
private fun KeyEntryScreenPreview() {
    MontageTheme { KeyEntryScreen(state = KeyEntrySamples.invalid, onIntent = {}) }
}

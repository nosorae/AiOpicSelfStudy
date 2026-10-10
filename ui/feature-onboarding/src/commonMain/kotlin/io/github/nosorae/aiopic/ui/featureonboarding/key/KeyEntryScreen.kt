package io.github.nosorae.aiopic.ui.featureonboarding.key

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import io.github.nosorae.aiopic.ui.design.LocalHaptics
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageButtonSize
import io.github.nosorae.aiopic.ui.design.MontageButtonVariant
import io.github.nosorae.aiopic.ui.design.MontageCelebration
import io.github.nosorae.aiopic.ui.design.MontageCelebrationSize
import io.github.nosorae.aiopic.ui.design.MontageCheckMark
import io.github.nosorae.aiopic.ui.design.MontageIconButton
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.design.MontageMotion
import io.github.nosorae.aiopic.ui.design.MontageRadius
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTextField
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import io.github.nosorae.aiopic.ui.design.icon_normal_circle_close_fill
import io.github.nosorae.aiopic.ui.design.icon_normal_external_link
import io.github.nosorae.aiopic.ui.featureonboarding.DomainError
import io.github.nosorae.aiopic.ui.featureonboarding.Res
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_checking
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_clear_key
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_error_invalid
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_error_retryable
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_free_tier_link
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_get_key
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_key
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_key_description
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_key_placeholder
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_provider
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_saved
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_step_copy
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_step_create
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_step_paste
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_steps_title
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_submit
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_title
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_why_body
import io.github.nosorae.aiopic.ui.featureonboarding.key_entry_why_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import io.github.nosorae.aiopic.ui.design.Res as DesignRes

/**
 * ViewModel 을 관측해 이동을 처리하는 바깥 겹. 키 저장 뒤 어디로 갈지는 화면을 등록한 쪽이 [onDone] 으로 정한다.
 * [onOpenFreeTierNotice] 는 음성 전송 동의 화면의 무료 등급 안내를 연다(F1.3).
 */
@Composable
fun KeyEntryRoute(
    viewModel: KeyEntryViewModel,
    onDone: () -> Unit,
    onOpenFreeTierNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 화면을 다시 그리는 사이 람다가 바뀌어도 이동 처리 중인 효과가 새 람다를 부르게 한다.
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.navigation) {
        if (state.navigation == KeyEntryDestination.KeySaved) {
            // 확인 체크가 다 그려지고 물결이 퍼질 때까지 보이고 넘어간다. 동작 줄이기여도 체크를 볼 틈은 같게 둔다.
            delay(CHECK_HOLD_MILLIS)
            currentOnDone()
            viewModel.onIntent(KeyEntryIntent.NavigationHandled)
        }
    }
    KeyEntryScreen(state = state, onIntent = viewModel::onIntent, onOpenFreeTierNotice = onOpenFreeTierNotice, modifier = modifier)
}

/** 상태만 받아 그리는 안쪽 겹. 미리보기와 스크린샷 테스트가 이 함수를 쓴다. */
@Composable
fun KeyEntryScreen(
    state: KeyEntryUiState,
    onIntent: (KeyEntryIntent) -> Unit,
    onOpenFreeTierNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gutter = MontageSpacing.spacing20
    val uriHandler = LocalUriHandler.current
    // 확인 중이거나 확인돼 넘어가기 직전이면 다른 입력을 막는다.
    val locked = state.checking || state.saved
    ErrorHaptics(state.error != null)
    MontageScreen(
        modifier = modifier,
        topBar = { MontageTopBar(title = stringResource(Res.string.key_entry_title)) },
        bottomBar = {
            Column(
                modifier = Modifier.padding(horizontal = gutter, vertical = MontageSpacing.spacing12),
                verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
            ) {
                if (state.saved) {
                    SavedCheck()
                } else {
                    if (state.checking) {
                        Text(
                            text = stringResource(Res.string.key_entry_checking, state.keyPageProvider.label),
                            style = MontageTheme.typography.label1Reading.regular,
                            color = MontageTheme.colors.foregroundNeutralSecondary,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    MontageButton(
                        text = stringResource(Res.string.key_entry_submit),
                        onClick = { onIntent(KeyEntryIntent.SubmitKey) },
                        // 확인하는 동안 버튼은 진행 표시를 보이고, 다른 입력(제공사, 입력칸, 키 받기, 안내)은 모두 막는다.
                        enabled = state.canSubmit || state.checking,
                        loading = state.checking,
                        modifier = Modifier.fillMaxWidth().testTag("key-submit"),
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = MontageSpacing.spacing16),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
        ) {
            WhyKeyCard(Modifier.padding(horizontal = gutter))
            SectionLabel(Res.string.key_entry_provider, Modifier.padding(start = gutter, end = gutter, top = MontageSpacing.spacing16))
            Column {
                state.providers.forEach { provider ->
                    MontageListCell(
                        title = provider.label,
                        selected = provider == state.provider,
                        enabled = !locked,
                        onClick = { onIntent(KeyEntryIntent.SelectProvider(provider)) },
                        modifier = Modifier.testTag("key-provider-${provider.id}"),
                    )
                }
            }
            SectionLabel(Res.string.key_entry_steps_title, Modifier.padding(horizontal = gutter))
            Column(
                modifier = Modifier.padding(horizontal = gutter),
                verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12),
            ) {
                Step(1, Res.string.key_entry_step_create)
                Step(2, Res.string.key_entry_step_copy)
                Step(3, Res.string.key_entry_step_paste)
                MontageButton(
                    text = stringResource(Res.string.key_entry_get_key, state.keyPageProvider.label),
                    onClick = { uriHandler.openUri(state.keyPageProvider.keyPageUrl) },
                    variant = MontageButtonVariant.Outlined,
                    enabled = !locked,
                    leadingIcon = DesignRes.drawable.icon_normal_external_link,
                    modifier = Modifier.fillMaxWidth().padding(top = MontageSpacing.spacing4),
                )
            }
            MontageTextField(
                value = state.key,
                onValueChange = { onIntent(KeyEntryIntent.ChangeKey(it)) },
                title = stringResource(Res.string.key_entry_key),
                placeholder = stringResource(Res.string.key_entry_key_placeholder),
                description = stringResource(
                    when (state.error) {
                        DomainError.InvalidKey -> Res.string.key_entry_error_invalid
                        DomainError.Unavailable -> Res.string.key_entry_error_retryable
                        null -> Res.string.key_entry_key_description
                    },
                ),
                error = state.error != null,
                enabled = !locked,
                // 키를 가리지 않는다. 40자 넘는 키를 붙여 넣은 뒤 다 들어갔는지 눈으로 보게 하려는 것이다.
                // 자동 수정과 자동 대문자가 키 글자를 바꾸지 않게 끈다.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                fontFamily = FontFamily.Monospace,
                trailing = if (state.key.isNotEmpty() && !locked) {
                    {
                        MontageIconButton(
                            icon = DesignRes.drawable.icon_normal_circle_close_fill,
                            contentDescription = stringResource(Res.string.key_entry_clear_key),
                            onClick = { onIntent(KeyEntryIntent.ChangeKey("")) },
                            tint = MontageTheme.colors.foregroundNeutralTertiary,
                        )
                    }
                } else {
                    null
                },
                modifier = Modifier.padding(start = gutter, end = gutter, top = MontageSpacing.spacing16).testTag("key-field"),
            )
            MontageButton(
                text = stringResource(Res.string.key_entry_free_tier_link),
                onClick = onOpenFreeTierNotice,
                variant = MontageButtonVariant.Text,
                enabled = !locked,
                size = MontageButtonSize.Medium,
                // 글자 버튼의 안쪽 여백 7dp 에 12dp 를 더해 글자를 다른 줄의 20dp 에 거의 맞춘다.
                modifier = Modifier.padding(horizontal = MontageSpacing.spacing12),
            )
        }
    }
}

/** 저장 버튼 자리에 버튼과 같은 높이로 체크와 결과 문구를 보인다. 키 저장 성공 진동은 체크가 다 그려질 때 여기서 낸다. 체크는 그림뿐이라 문구를 화면 읽기 도구가 읽게 한다. */
@Composable
private fun SavedCheck() {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = MontageSpacing.spacing48),
        horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing4, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 체크가 다 그려지는 때 작은 축하가 터지며 성공 진동을 낸다. 조각은 체크보다 넓게 퍼지되 줄 높이를 바꾸지 않는다.
        Box(Modifier.size(MontageSpacing.spacing48), contentAlignment = Alignment.Center) {
            MontageCheckMark(Modifier.fillMaxSize())
            MontageCelebration(MontageCelebrationSize.Small, Modifier.requiredSize(MontageSpacing.spacing48 * 2), delayMillis = MontageMotion.LONG_MILLIS)
        }
        Text(
            text = stringResource(Res.string.key_entry_saved),
            style = MontageTheme.typography.body1.bold,
            color = MontageTheme.colors.foregroundBrandPrimary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** 키 확인 오류 안내가 새로 뜨면 오류 진동을 낸다. 처음 그릴 때와 화면을 돌려 다시 그릴 때는 바뀐 것이 없어 내지 않는다. */
@Composable
private fun ErrorHaptics(error: Boolean) {
    val haptics = LocalHaptics.current
    val last = remember { booleanArrayOf(error) }
    LaunchedEffect(error) {
        if (error && !last[0]) haptics.play(HapticEffect.Error)
        last[0] = error
    }
}

/** 서버 없이 사용자 키로 직접 보낸다는 것과 요금이 어디에 잡히는지를 먼저 알린다. */
@Composable
private fun WhyKeyCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MontageTheme.colors.surfaceBrandPrimaryOpacity8, RoundedCornerShape(MontageRadius.radius16))
            .padding(MontageSpacing.spacing16),
        verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing4),
    ) {
        Text(
            text = stringResource(Res.string.key_entry_why_title),
            style = MontageTheme.typography.body1.bold,
            color = MontageTheme.colors.foregroundNeutralPrimary,
        )
        Text(
            text = stringResource(Res.string.key_entry_why_body),
            style = MontageTheme.typography.label1Reading.regular,
            color = MontageTheme.colors.foregroundNeutralSecondary,
        )
    }
}

@Composable
private fun SectionLabel(text: StringResource, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(text),
        style = MontageTheme.typography.label1.bold,
        color = MontageTheme.colors.foregroundNeutralSecondary,
        modifier = modifier,
    )
}

/** 번호 원과 한 줄 설명. 원은 글자 크기를 키우면 같이 커지도록 최소 크기만 정한다. */
@Composable
private fun Step(number: Int, text: StringResource) {
    Row(horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .sizeIn(minWidth = MontageSpacing.spacing24, minHeight = MontageSpacing.spacing24)
                .background(MontageTheme.colors.surfaceBrandPrimary, CircleShape)
                .padding(horizontal = MontageSpacing.spacing4),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number.toString(), style = MontageTheme.typography.caption1.bold, color = MontageTheme.colors.onBrand)
        }
        Text(
            text = stringResource(text),
            style = MontageTheme.typography.body2.regular,
            color = MontageTheme.colors.foregroundNeutralPrimary,
        )
    }
}

/** 확인 체크를 그리고(400ms) 물결이 퍼지는(400ms) 동안 머문다. */
private const val CHECK_HOLD_MILLIS = MontageMotion.LONG_MILLIS * 3L

/** 미리보기와 스크린샷 테스트가 함께 쓰는 고정 상태. */
internal object KeyEntrySamples {
    val empty = KeyEntryUiState()
    val ready = KeyEntryUiState(selectedProvider = KeyProvider.Gemini, key = "test-key")
    val checking = ready.copy(checking = true)
    val saved = ready.copy(saved = true)
    val invalid = ready.copy(error = DomainError.InvalidKey)
    val retryable = ready.copy(error = DomainError.Unavailable)
}

@Preview
@Composable
private fun KeyEntryScreenPreview() {
    MontageTheme { KeyEntryScreen(state = KeyEntrySamples.invalid, onIntent = {}, onOpenFreeTierNotice = {}) }
}

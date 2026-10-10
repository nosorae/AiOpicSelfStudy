package io.github.nosorae.aiopic.ui.featureonboarding.consent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import io.github.nosorae.aiopic.ui.featureonboarding.Res
import io.github.nosorae.aiopic.ui.featureonboarding.consent_agree
import io.github.nosorae.aiopic.ui.featureonboarding.consent_eligibility
import io.github.nosorae.aiopic.ui.featureonboarding.consent_free_tier
import io.github.nosorae.aiopic.ui.featureonboarding.consent_revoke
import io.github.nosorae.aiopic.ui.featureonboarding.consent_send
import io.github.nosorae.aiopic.ui.featureonboarding.consent_title
import org.jetbrains.compose.resources.stringResource

/** 화면에 보이는 안내 순서. 무료 등급 안내(F1.3)는 굵게 둔다. 키 입력, 모델 선택 화면은 이 화면으로 연결만 한다. */
internal val consentNotices = listOf(Res.string.consent_send, Res.string.consent_free_tier, Res.string.consent_eligibility, Res.string.consent_revoke)

/** ViewModel 을 관측해 이동을 처리하는 바깥 겹. 동의 뒤 어디로 갈지는 화면을 등록한 쪽이 [onDone] 으로 정한다. */
@Composable
fun ConsentRoute(viewModel: ConsentViewModel, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.navigation) {
        if (state.navigation == ConsentDestination.ConsentGiven) {
            currentOnDone()
            viewModel.onIntent(ConsentIntent.NavigationHandled)
        }
    }
    ConsentScreen(onIntent = viewModel::onIntent, modifier = modifier)
}

/** 그리기만 하는 안쪽 겹. 그릴 상태가 없어 [onIntent] 만 받는다. 동의는 아래 버튼으로만 한다. 뒤로가기는 가로채지 않는다. */
@Composable
fun ConsentScreen(onIntent: (ConsentIntent) -> Unit, modifier: Modifier = Modifier) {
    val gutter = MontageSpacing.spacing20
    MontageScreen(
        modifier = modifier,
        topBar = { MontageTopBar(title = stringResource(Res.string.consent_title)) },
        bottomBar = {
            MontageButton(
                text = stringResource(Res.string.consent_agree),
                onClick = { onIntent(ConsentIntent.GiveConsent) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = MontageSpacing.spacing12).testTag("consent-agree"),
            )
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = gutter, vertical = MontageSpacing.spacing16),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing16),
        ) {
            consentNotices.forEach { notice ->
                val style = MontageTheme.typography.body1Reading
                Text(
                    text = stringResource(notice),
                    style = if (notice == Res.string.consent_free_tier) style.bold else style.regular,
                    color = MontageTheme.colors.foregroundNeutralPrimary,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ConsentScreenPreview() {
    MontageTheme { ConsentScreen(onIntent = {}) }
}

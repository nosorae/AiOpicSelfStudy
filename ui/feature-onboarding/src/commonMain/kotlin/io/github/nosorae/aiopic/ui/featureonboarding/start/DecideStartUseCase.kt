package io.github.nosorae.aiopic.ui.featureonboarding.start

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.ui.featureonboarding.consent.VoiceConsent
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider

/** 앱을 켤 때 가는 곳(F1.0). 키 입력(S1), 동의(S2), 홈 순서라 동의는 홈 이후의 마이크 권한 요청보다 늘 먼저다. */
enum class StartDestination { KeyEntry, Consent, Home }

/** 저장된 키와 동의 상태만 읽어 갈 곳을 정한다(F1.0, ADR 0015). 키가 맞는지 묻는 요청은 보내지 않는다. */
@Inject
class DecideStartUseCase(
    private val keyVault: KeyVault,
    private val voiceConsent: VoiceConsent,
) {
    suspend operator fun invoke(): StartDestination = when {
        KeyProvider.entries.none { keyVault.get(it.id) != null } -> StartDestination.KeyEntry
        !voiceConsent.isGiven() -> StartDestination.Consent
        else -> StartDestination.Home
    }
}

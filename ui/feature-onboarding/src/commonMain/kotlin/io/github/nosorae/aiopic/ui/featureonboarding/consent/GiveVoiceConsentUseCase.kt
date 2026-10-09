package io.github.nosorae.aiopic.ui.featureonboarding.consent

import dev.zacsweers.metro.Inject

/** 사용자가 동의 버튼을 눌렀을 때만 부른다(F1.3). 뒤로가기나 홈으로 나가는 길에는 부르지 않는다. */
@Inject
class GiveVoiceConsentUseCase(private val voiceConsent: VoiceConsent) {
    suspend operator fun invoke() = voiceConsent.give()
}

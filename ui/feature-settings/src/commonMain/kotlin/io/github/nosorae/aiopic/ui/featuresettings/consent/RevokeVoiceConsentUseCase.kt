package io.github.nosorae.aiopic.ui.featuresettings.consent

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.ui.featureonboarding.consent.VoiceConsent

/** 동의를 철회할 때 남은 분석을 보내지 않고 끝낸다. 앱 층이 분석 작업 표를 잇고, 테스트는 가짜를 쓴다. */
fun interface PendingAnalysis {
    suspend fun cancelAll()
}

/**
 * 음성 전송 동의를 철회한다(F1.3). 동의를 먼저 지워서 이미 보내려던 작업도 동의를 다시 읽고 보내지 않게 하고,
 * 남은 분석 작업도 동의 없음으로 끝낸다. 답변과 녹음은 기기에 남는다. 다음 실행의 시작 분기가 동의 화면으로 간다.
 */
@Inject
class RevokeVoiceConsentUseCase(private val voiceConsent: VoiceConsent, private val pendingAnalysis: PendingAnalysis) {
    suspend operator fun invoke() {
        voiceConsent.revoke()
        pendingAnalysis.cancelAll()
    }
}

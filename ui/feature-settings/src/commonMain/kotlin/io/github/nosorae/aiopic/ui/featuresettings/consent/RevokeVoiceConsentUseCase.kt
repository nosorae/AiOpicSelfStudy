package io.github.nosorae.aiopic.ui.featuresettings.consent

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.libs.workqueue.api.WorkQueue
import io.github.nosorae.aiopic.ui.featureonboarding.consent.VoiceConsent

/**
 * 음성 전송 동의를 철회한다(F1.3). 동의를 먼저 지워서 이미 꺼낸 작업도 분석 처리기가 동의를 다시 읽고 보내지 않게 하고,
 * 대기열에 남은 답변도 버린다. 답변과 녹음은 기기에 남는다. 다음 실행의 시작 분기가 동의 화면으로 간다.
 */
@Inject
class RevokeVoiceConsentUseCase(private val voiceConsent: VoiceConsent, private val workQueue: WorkQueue) {
    suspend operator fun invoke() {
        voiceConsent.revoke()
        workQueue.cancelAll()
    }
}

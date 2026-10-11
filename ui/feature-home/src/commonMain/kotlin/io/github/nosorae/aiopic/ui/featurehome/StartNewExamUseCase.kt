package io.github.nosorae.aiopic.ui.featurehome

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import kotlinx.coroutines.flow.first

/**
 * 새 회차를 시작하기 전에 이어 볼 수 있던 회차를 미완료로 끝낸다(#223). 답변과 녹음은 그대로 두어 기록과 리포트에 남는다.
 * 새 회차는 시험 화면이 열리자마자 진행 중으로 저장되므로, 진행 중인 회차 전부가 아니라 [sessionId] 하나만 끝낸다.
 */
@Inject
class StartNewExamUseCase(private val examSessionSource: ExamSessionSource) {
    suspend operator fun invoke(sessionId: String) {
        val session = examSessionSource.sessions().first().firstOrNull { it.id == sessionId && it.status == ExamStatus.InProgress } ?: return
        examSessionSource.save(session.copy(status = ExamStatus.Abandoned))
    }
}

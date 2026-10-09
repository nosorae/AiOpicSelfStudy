package io.github.nosorae.aiopic.ui.featurereport.flag

import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import kotlinx.coroutines.flow.Flow

/**
 * "틀림" 표시 저장소(F8.1). 앱 층이 기기 저장소에 이어 주고, 테스트는 메모리 가짜를 쓴다.
 * 표시는 답변 id 와 `Feedback.corrections` 안의 순서만 남긴다. 전문, 교정 내용, 분석 모델은 답변에 이미 있어서
 * 기록의 약점 점수(F8.2)와 설정의 내보내기(F8.3)가 답변과 함께 읽는다.
 */
interface FlagStore {
    fun flags(): Flow<Set<CorrectionFlag>>

    suspend fun mark(flag: CorrectionFlag, wrong: Boolean)
}

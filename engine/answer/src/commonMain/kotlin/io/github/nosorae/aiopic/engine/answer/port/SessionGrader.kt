package io.github.nosorae.aiopic.engine.answer.port

import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import io.github.nosorae.aiopic.engine.session.model.SessionGrade

/**
 * 회차 등급(F5.3, ADR 0011). 15번이 끝나면 답변 피드백과 녹음을 묶어 한 번 보내고 체크리스트, 등급 범위, 근거, 무너진 문항을 받는다.
 * 답변 피드백은 부르는 쪽이 [prompt] 본문에 채워 넣는다. [recordings] 는 답한 순서이고 그 수가 문항 수다.
 * [scale] 은 출력 검사(F5.4)가 받은 등급이 등급 체계 안에 있는지 보는 데 쓴다.
 */
interface SessionGrader {
    suspend fun grade(model: ModelInfo, prompt: Prompt, scale: GradeScale, recordings: List<Recording>): AnalysisResult<SessionGrade>
}

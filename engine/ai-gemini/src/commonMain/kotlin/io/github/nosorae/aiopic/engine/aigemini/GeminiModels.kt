package io.github.nosorae.aiopic.engine.aigemini

import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog

/**
 * 앱이 들고 있는 Gemini 분석 모델 목록(F5.8, ADR 0019). 녹음을 넣고 구조화 출력으로 실제 응답을 받아 본 모델만 둔다.
 * 기본은 미리 확인(`s1`)에서 멈춤과 말 속도를 재 본 모델이다.
 */
object GeminiModels : ModelCatalog {
    /** 키 보관과 답변 결과의 제공사 이름. */
    const val PROVIDER = "gemini"

    override val defaultModel = ModelInfo(PROVIDER, "gemini-3.8-flash", "Gemini 3.8 Flash", hearsAudio = true, stability = Stability.Stable)

    // ponytail: 두 개만 둔다. 품질 확인 도구(n-eval)로 출력 검사 통과를 본 모델이 생기면 여기에 더한다.
    override val models = listOf(
        defaultModel,
        ModelInfo(PROVIDER, "gemini-3.5-flash-lite", "Gemini 3.5 Flash Lite", hearsAudio = true, stability = Stability.Stable),
    )
}

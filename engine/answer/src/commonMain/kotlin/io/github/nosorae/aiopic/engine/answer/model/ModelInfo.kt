package io.github.nosorae.aiopic.engine.answer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 앱이 들고 있는 검증된 분석 모델 목록의 항목 하나(F5.8, ADR 0019). 제공사 목록 응답에는 오디오 입력을 받는지가 없어서
 * [hearsAudio] 를 앱이 정해 둔다.
 */
@Serializable
data class ModelInfo(
    val provider: String,
    val id: String,
    val displayName: String,
    /** 음성을 직접 들으면 한 번 호출 경로, 아니면 받아쓰기 후 글 분석 경로로 간다. */
    val hearsAudio: Boolean,
    val stability: Stability,
)

@Serializable
enum class Stability {
    @SerialName("stable")
    Stable,

    /** 2주 공지로 내려갈 수 있어서 고를 때 안내한다. */
    @SerialName("preview")
    Preview,
}

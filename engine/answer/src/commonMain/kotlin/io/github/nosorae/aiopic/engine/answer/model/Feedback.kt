package io.github.nosorae.aiopic.engine.answer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/** 답변 하나의 피드백(F5.2). 등급은 여기 없고 회차에 한 번 붙는다(ADR 0011). */
@Serializable
data class Feedback(
    val transcript: String,
    val corrections: List<Correction>,
    /** 한 등급 위 표현. */
    val upgrades: List<Upgrade>,
    /** 시제를 처음부터 끝까지 맞게 유지했는지. */
    val tenseConsistent: Boolean,
    /** 문단 길이. 문장 수로 센다. */
    val sentenceCount: Int,
    val wordsPerMinute: Double,
    val pauses: List<Pause>,
    /** 롤플레이에서 사용자가 한 질문 수. 롤플레이가 아닌 문항이면 null 이다. */
    val roleplayQuestionCount: Int?,
    val pronunciation: Pronunciation,
    /** 0~100. 같은 문항 유형끼리 모아 약점 점수를 낸다. */
    val score: Int,
)

/** [original] 은 전문에 그대로 있는 구절이어야 한다. 앱 코드가 확인한다(F5.4). */
@Serializable
data class Correction(val original: String, val corrected: String)

@Serializable
data class Upgrade(val original: String, val better: String)

/** 말 멈춤 하나. 녹음 시작부터 잰 위치와 길이다. */
@Serializable
data class Pause(val start: Duration, val length: Duration)

@Serializable
sealed interface Pronunciation {
    /** 음성을 듣지 않고 받아쓴 글만 분석한 경로라서 발음을 보지 않았다(F5.9). */
    @Serializable
    @SerialName("not_evaluated")
    data object NotEvaluated : Pronunciation

    @Serializable
    @SerialName("evaluated")
    data class Evaluated(val comment: String) : Pronunciation
}

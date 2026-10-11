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
    /** 표현은 맞아도 내용이 어색한 곳(#217). 이 항목을 받기 전에 분석한 답변은 빈 목록이고 화면이 칸을 숨긴다. */
    val contentFixes: List<ContentFix> = emptyList(),
    /** 사용자의 답을 살려 표현과 내용을 모두 고친 영어 답변(#217). 이 항목을 받기 전에 분석한 답변은 null 이다. */
    val finalScript: String? = null,
)

/**
 * [original] 은 전문에 그대로 있는 구절이어야 한다. 앱 코드가 확인한다(F5.4).
 * [deducted] 는 이 교정 때문에 점수에서 깎은 몫이다(ADR 0027). 사용자가 틀림으로 표시하면 약점 계산이 이만큼 되돌린다(F8.2).
 * 프롬프트가 이 값을 받기 전에 저장한 답변과 출력 검사를 통과 못한 값은 null 이다.
 */
@Serializable
data class Correction(val original: String, val corrected: String, val deducted: Int? = null)

/**
 * 내용 교정 하나(#217). 질문과 안 맞는 답, 이어지지 않는 흐름, 이유나 예시가 빠진 주장, 뒤섞인 시점처럼 문법으로는 못 잡는 것이다.
 * [original] 은 전문에 그대로 있는 구절이고, [problem] 은 무엇이 어색한지 한국어 한 문장, [corrected] 는 고친 영어 문장이다.
 */
@Serializable
data class ContentFix(val original: String, val problem: String, val corrected: String)

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

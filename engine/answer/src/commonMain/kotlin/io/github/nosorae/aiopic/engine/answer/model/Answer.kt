package io.github.nosorae.aiopic.engine.answer.model

import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/** 답변 하나. 모의고사와 한 문항 연습이 같은 모양으로 남기고, 리포트와 다음 출제는 이것을 쌓아 본다. */
@Serializable
data class Answer(
    val id: String,
    val question: Question,
    /** 어느 모의고사 회차에서 나왔는지. 한 문항 연습이면 null 이다. */
    val examSessionId: String?,
    /** 녹음 조각 파일 경로, 녹음한 순서대로(F3.2). 파일은 기기에만 둔다. */
    val chunkPaths: List<String>,
    /** 이어 녹음을 시작한 위치(이어 붙인 녹음 기준). 분석이 이 구간을 말 멈춤으로 세지 않게 요청에 표시한다(F3.3, F3.4). */
    val resumedAt: List<Duration> = emptyList(),
    val analysis: AnswerAnalysis = AnswerAnalysis.Pending,
)

/** 답변 분석 결과. 분석이 끝나지 않은 답변도 녹음은 남는다(F5.6). */
@Serializable
sealed interface AnswerAnalysis {
    /** 분석 작업 표(#205)에서 기다리거나 보내는 중이다. 무엇을 기다리는지는 작업 표가 안다. 작업이 없는 기다림은 앱을 켤 때 다시 넣는다(`AnalysisJobs.recover`). */
    @Serializable
    @SerialName("pending")
    data object Pending : AnswerAnalysis

    @Serializable
    @SerialName("done")
    data class Done(val analyzedBy: AnalyzedBy, val feedback: Feedback) : AnswerAnalysis

    /**
     * 보냈지만 결과를 받지 못하고 분석 작업이 끝났다(#203, #205). 화면은 기다림을 끝내고 [error] 에 맞는 이유와 다시 분석하기를 보인다.
     * 다시 보낼 실패는 작업이 횟수를 다 쓴 뒤에야 여기로 온다.
     */
    @Serializable
    @SerialName("failed")
    data class Failed(val error: AnalysisError) : AnswerAnalysis

    /** 모델이 평가할 수 없다고 이유 코드를 돌려줬다. 화면에는 "평가할 수 없음"으로 보인다(F5.5). */
    @Serializable
    @SerialName("unscorable")
    data class Unscorable(val analyzedBy: AnalyzedBy, val reason: UnscorableReason) : AnswerAnalysis
}

/**
 * 어느 분석이 이 결과를 냈는지(F5.7). 모델마다 채점 경향이 달라서, 기록 화면의 등급 변화가
 * 모델 교체 때문인지 실력 때문인지 가리려고 답변마다 남긴다.
 */
@Serializable
data class AnalyzedBy(
    /** 제공사 이름. 키 보관과 모델 목록이 같은 이름을 쓴다. */
    val provider: String,
    val modelId: String,
    val promptVersion: String,
)

@Serializable
enum class UnscorableReason {
    @SerialName("silent")
    Silent,

    /** 시험 언어가 아닌 말로 답했다. */
    @SerialName("wrong_language")
    WrongLanguage,

    /** 5초 미만이다. */
    @SerialName("too_short")
    TooShort,
}

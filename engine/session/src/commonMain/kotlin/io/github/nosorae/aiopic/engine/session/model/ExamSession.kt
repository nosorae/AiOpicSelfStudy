package io.github.nosorae.aiopic.engine.session.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/** 모의시험 한 회차의 진행 상태. 바뀔 때마다 통째로 저장하고 앱을 켜면 여기서 다시 시작한다(F3.1, ADR 0012). */
@Serializable
data class ExamSession(
    val id: String,
    /** [ExamSpec.steps] 안의 지금 단계. */
    val stepIndex: Int = 0,
    val surveyTopics: List<String> = emptyList(),
    val level: Int? = null,
    val recalibration: Recalibration? = null,
    /** 답한 순서대로의 답변 id. */
    val answerIds: List<String> = emptyList(),
    /** 남은 시험 시간. 앱이 꺼져 있는 동안 줄지 않는다(F2.5). 시간이 시작되기 전에는 null 이다. */
    val remaining: Duration? = null,
    /** 지금 문항에서 다시 듣기를 썼는지(F2.4, F3.3). */
    val replayUsed: Boolean = false,
    val status: ExamStatus = ExamStatus.InProgress,
    val grade: SessionGrade? = null,
)

@Serializable
enum class ExamStatus {
    @SerialName("in_progress")
    InProgress,

    @SerialName("finished")
    Finished,

    /** 시간이 다 됐다. 답한 문항까지만 채점하고 남은 문항은 "시간 부족"이다(F2.6). */
    @SerialName("time_up")
    TimeUp,
}

/** 난이도 재조정에서 고른 것. */
@Serializable
enum class Recalibration {
    @SerialName("easier")
    Easier,

    @SerialName("same")
    Same,

    @SerialName("harder")
    Harder,
}

/**
 * 회차 등급(F5.3, ADR 0011). 답변 점수 평균으로는 한 문항에서 무너진 것을 못 잡아서
 * 등급 하나 대신 범위와 근거, 무너진 문항을 받는다.
 */
@Serializable
data class SessionGrade(
    /** 등급 기준 문장마다 채운 예/아니오. 범위와 근거가 이 체크에서 나온다. 같은 입력으로 다시 돌렸을 때 어느 문장에서 갈렸는지 본다. */
    val checks: List<GradeCheck>,
    /** [GradeScale] 의 등급 이름. */
    val low: String,
    val high: String,
    val reasons: List<String>,
    /** 무너진 문항의 번호(1 부터). */
    val brokeAt: List<Int>,
)

/** 등급 기준 문장 하나의 체크 결과. [id] 는 시험 팩 프롬프트의 문장 번호다. */
@Serializable
data class GradeCheck(val id: String, val met: Boolean)

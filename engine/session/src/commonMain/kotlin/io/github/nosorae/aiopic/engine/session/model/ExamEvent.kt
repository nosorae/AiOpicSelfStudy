package io.github.nosorae.aiopic.engine.session.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/** 시험 진행에서 일어난 일. 진행 계산 함수가 (상태, 일어난 일)로 다음 상태를 낸다(F3.1). */
@Serializable
sealed interface ExamEvent {
    @Serializable
    @SerialName("survey_submitted")
    data class SurveySubmitted(val topics: List<String>) : ExamEvent

    @Serializable
    @SerialName("level_selected")
    data class LevelSelected(val level: Int) : ExamEvent

    @Serializable
    @SerialName("play")
    data object Play : ExamEvent

    @Serializable
    @SerialName("replay")
    data object Replay : ExamEvent

    /** 다음 단계로. 채점하는 문항에서 누르면 그 문항의 [answerId] 를 답한 순서에 붙인다. */
    @Serializable
    @SerialName("next")
    data class Next(val answerId: String? = null) : ExamEvent

    @Serializable
    @SerialName("recalibrated")
    data class Recalibrated(val choice: Recalibration) : ExamEvent

    @Serializable
    @SerialName("app_closed")
    data object AppClosed : ExamEvent

    @Serializable
    @SerialName("time_up")
    data object TimeUp : ExamEvent

    /**
     * 문항 화면에 있는 동안 단조 시계로 잰 흐른 시간. 녹음 조각이 끝날 때마다 보내서 앱이 갑자기 죽어도 그 조각까지는 남는다(ADR 0012).
     * 문항 화면을 벗어난 동안에는 보내지 않아서 그동안 40분 타이머가 멈춘다(F2.5).
     */
    @Serializable
    @SerialName("time_passed")
    data class TimePassed(val duration: Duration) : ExamEvent
}

package io.github.nosorae.aiopic.engine.session.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

    @Serializable
    @SerialName("next")
    data object Next : ExamEvent

    @Serializable
    @SerialName("recalibrated")
    data class Recalibrated(val choice: Recalibration) : ExamEvent

    @Serializable
    @SerialName("app_closed")
    data object AppClosed : ExamEvent

    @Serializable
    @SerialName("time_up")
    data object TimeUp : ExamEvent
}

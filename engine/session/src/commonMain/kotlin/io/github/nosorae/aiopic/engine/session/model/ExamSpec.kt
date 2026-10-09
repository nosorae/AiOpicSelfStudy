package io.github.nosorae.aiopic.engine.session.model

import io.github.nosorae.aiopic.engine.question.model.QuestionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/** 시험 한 회의 모양. 단계 순서를 데이터로 두어 엔진은 시험 이름을 모르고 시험 팩이 채운다(F2.1). */
@Serializable
data class ExamSpec(
    /** 첫 [ExamStep.Ask] 에서 시작하는 시험 시간. */
    val timeLimit: Duration,
    /** 수준 선택 단계에서 고를 수 있는 수준 수. 1 부터 센다. */
    val levelCount: Int,
    val steps: List<ExamStep>,
)

@Serializable
sealed interface ExamStep {
    /** [minTopics] 개 미만으로 고르면 다음으로 못 간다(F2.2). */
    @Serializable
    @SerialName("survey")
    data class Survey(val minTopics: Int) : ExamStep

    @Serializable
    @SerialName("level_select")
    data object LevelSelect : ExamStep

    /** 마이크 점검과 채점하지 않는 연습 문항. */
    @Serializable
    @SerialName("check")
    data object Check : ExamStep

    /** 채점하는 문항 하나. 고른 수준이 [minLevel] 보다 낮으면 건너뛴다(낮은 수준은 문항이 적다). */
    @Serializable
    @SerialName("ask")
    data class Ask(val questionType: QuestionType, val minLevel: Int = 1) : ExamStep

    @Serializable
    @SerialName("recalibrate")
    data object Recalibrate : ExamStep
}

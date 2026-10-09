package io.github.nosorae.aiopic.packs.opicen

import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

/** 영어 오픽 한 회의 순서와 등급. 엔진은 이 값을 데이터로만 받는다(F2.1). */
object OpicEn {
    private val surveySet = listOf(OpicQuestionType.Description, OpicQuestionType.Routine, OpicQuestionType.PastExperience)

    private fun ask(type: OpicQuestionType, minLevel: Int = 1) = ExamStep.Ask(type.type, minLevel)

    // ponytail: 수준 1~2 에서 빠지는 문항을 13~15번으로 잡았다. 실제 시험에서 어느 문항이 빠지는지 확인되면 minLevel 만 옮긴다.

    /**
     * 자기소개, 서베이 주제 세트 둘, 재조정, 세트 하나, 롤플레이 셋, 비교와 사회 이슈 순이다.
     * 수준 1~2 는 12문항이라 마지막 세 문항을 건너뛴다.
     */
    val examSpec = ExamSpec(
        timeLimit = 40.minutes,
        levelCount = 6,
        steps = listOf(ExamStep.Survey(minTopics = 12), ExamStep.LevelSelect, ExamStep.Check) +
            ask(OpicQuestionType.SelfIntro) +
            (surveySet + surveySet).map { ask(it) } +
            ExamStep.Recalibrate +
            surveySet.map { ask(it) } +
            ask(OpicQuestionType.RoleplayAsk) +
            ask(OpicQuestionType.RoleplaySolve) +
            ask(OpicQuestionType.RoleplayExperience, minLevel = 3) +
            ask(OpicQuestionType.Comparison, minLevel = 3) +
            ask(OpicQuestionType.SocialIssue, minLevel = 3),
    )

    val gradeScale = GradeScale(OpicGrade.entries.map { it.name })
}

/** 오픽 등급, 낮은 것부터. 회차 등급 응답 스키마가 이 값만 고르게 한다(F5.3). */
@Serializable
enum class OpicGrade { NL, NM, NH, IL, IM1, IM2, IM3, IH, AL }

/** 오픽 문항 유형. JSON 이름이 엔진의 [QuestionType] id 이고 출제 응답 스키마가 이 값만 고르게 한다(F6.1). */
@Serializable
enum class OpicQuestionType {
    @SerialName("self-intro")
    SelfIntro,

    @SerialName("description")
    Description,

    @SerialName("routine")
    Routine,

    @SerialName("past-experience")
    PastExperience,

    /** 상황 속 상대에게 질문을 해서 정보를 얻는다. */
    @SerialName("roleplay-ask")
    RoleplayAsk,

    /** 문제 상황을 설명하고 대안을 낸다. */
    @SerialName("roleplay-solve")
    RoleplaySolve,

    @SerialName("roleplay-experience")
    RoleplayExperience,

    @SerialName("comparison")
    Comparison,

    @SerialName("social-issue")
    SocialIssue,
    ;

    val type: QuestionType get() = QuestionType(serializer().descriptor.getElementName(ordinal))
}

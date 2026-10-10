package io.github.nosorae.aiopic.packs.opicen

import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.question.pick.parseQuestions
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

    /**
     * 서베이 주제 고르기 앞의 Part 1~3 질문과 보기(F2.2). 화면 모듈은 팩을 모르므로 질문 글자와 보기 목록 쌍으로 두고 앱이 화면 타입으로 옮긴다.
     * 답은 출제에 쓰지 않는다. 실제 시험과 같은 순서를 밟아 보게 하려는 화면이다.
     */
    val surveyParts: List<Pair<String, List<String>>> = listOf(
        "현재 귀하는 어느 분야에 종사하고 계십니까?" to listOf("사업, 회사", "재택근무, 재택사업", "교사, 교육자", "일 경험 없음"),
        "현재 귀하는 학생이십니까?" to listOf("예", "아니요"),
        "현재 귀하는 어디에 살고 계십니까?" to listOf(
            "개인주택이나 아파트에 홀로 거주",
            "친구나 룸메이트와 함께 주택이나 아파트에 거주",
            "가족과 함께 주택이나 아파트에 거주",
            "학교 기숙사",
            "군대 막사",
        ),
    )

    /** 서베이에서 고를 수 있는 주제. 내장 문항 풀의 주제 id 와 같아야 그 주제 문항이 나온다(F6.2). */
    // ponytail: 화면에 주제 id 를 그대로 보인다. 한국어 이름은 문구 노드(u-copy)가 붙인다.
    val surveyTopics = listOf(
        "movies", "music", "parks", "cafes", "shopping", "overseas-travel", "home",
        "jogging", "tv", "walking", "cooking", "domestic-travel", "beach", "neighborhood",
    )

    /**
     * 모델이 만든 질문에서 서베이 밖 주제를 몇 개까지 받는지(F6.1). 출제 프롬프트 규칙대로 자기소개(self), 서베이 밖 세트 하나,
     * 롤플레이 상황, 비교와 사회 이슈 주제가 서베이 밖일 수 있다. 넘으면 내장 문항으로 고른다(F6.2).
     */
    const val MAX_OUTSIDE_TOPICS = 4

    /** 내장 문항 풀(F6.2). 앱이 출제할 때마다 읽는다. */
    suspend fun questionPool(): List<Question> = parseQuestions(Res.readBytes("files/questions/pool.json").decodeToString())
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

package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.Recalibration
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** 녹음 시작 뒤 다시 듣기를 할 수 있는 시간(F2.4). */
val ReplayWindow = 5.seconds

/** 녹음 시작 뒤 Next 가 켜지기까지의 시간(F2.4). */
val NextDelay = 10.seconds

data class ExamUiState(
    /** 지금 단계. 회차를 읽기 전에는 null 이다. */
    val step: ExamStep? = null,
    val surveyParts: List<SurveyPart> = emptyList(),
    /** Part 위치마다 고른 답의 위치. */
    val surveyAnswers: Map<Int, Int> = emptyMap(),
    val topics: List<String> = emptyList(),
    val selectedTopics: Set<String> = emptySet(),
    val levelCount: Int = 0,
    val selectedLevel: Int? = null,
    val selectedRecalibration: Recalibration? = null,
    /** 채점 문항의 번호(1 부터)와 이번 회차 문항 수. 고른 수준에서 건너뛰는 문항은 세지 않는다. */
    val questionNumber: Int = 0,
    val questionCount: Int = 0,
    /** 남은 시험 시간. 1번 문항 전에는 null 이다. */
    val remaining: Duration? = null,
    val replayUsed: Boolean = false,
    val phase: QuestionPhase = QuestionPhase.Ready,
    /** 지금 녹음이 시작된 뒤 흐른 시간. */
    val recorded: Duration = Duration.ZERO,
    /** 문항 화면에 [recorded] 를 보일지(F2.7). 기본은 꺼짐이고 설정에서 켠다. */
    val answerTimer: Boolean = false,
    val error: ExamError? = null,
    /** 누른 버튼(다음, Next, 나가기)의 저장이 도는 중이다. 끝날 때까지 그 버튼은 진행 표시를 보이고 다른 입력은 받지 않는다. */
    val saving: Boolean = false,
    val navigation: ExamDestination? = null,
) {
    val canSubmitSurvey: Boolean get() = (step as? ExamStep.Survey)?.let { selectedTopics.size >= it.minTopics } ?: false
    val canReplay: Boolean get() = phase == QuestionPhase.Recording && !replayUsed && recorded < ReplayWindow && !saving
    val canNext: Boolean get() = phase == QuestionPhase.Recording && recorded >= NextDelay
}

/** 문항 화면의 진행. 질문 글자는 보이지 않고 읽기와 녹음만 있다(F2.3). */
enum class QuestionPhase { Ready, Speaking, Recording }

enum class ExamError {
    /** 마이크 권한이 없거나 마이크를 열지 못했다. */
    MicUnavailable,

    /** 기기 음성으로 질문을 읽지 못했다. */
    SpeechFailed,

    /** 낼 질문을 얻지 못했다. */
    QuestionUnavailable,
}

/** 저장이 끝나야 가는 이동만 상태로 둔다(ADR 0009). 어디로 갈지는 화면을 등록한 쪽이 정한다. */
enum class ExamDestination {
    /** 회차를 남긴 채 나간다. 홈에서 이어 본다. */
    Exit,

    /** 마지막 문항을 냈거나 시간이 다 됐다. */
    Finished,
}

sealed interface ExamIntent {
    data class SelectSurveyAnswer(val part: Int, val option: Int) : ExamIntent

    data class ToggleTopic(val topic: String) : ExamIntent

    data object SubmitSurvey : ExamIntent

    data class SelectLevel(val level: Int) : ExamIntent

    data object SubmitLevel : ExamIntent

    data class SelectRecalibration(val choice: Recalibration) : ExamIntent

    data object SubmitRecalibration : ExamIntent

    data object PlayQuestion : ExamIntent

    data object ReplayQuestion : ExamIntent

    data object SubmitAnswer : ExamIntent

    /** 화면이 마이크 권한을 요청했는데 거절됐다. */
    data object MicDenied : ExamIntent

    data object ExitExam : ExamIntent

    data object NavigationHandled : ExamIntent
}

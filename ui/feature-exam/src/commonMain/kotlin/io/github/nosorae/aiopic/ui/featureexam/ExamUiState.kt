package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.Recalibration
import io.github.nosorae.aiopic.libs.recorder.api.RecordingFailure
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** 녹음 시작 뒤 다시 듣기를 할 수 있는 시간(F2.4). */
val ReplayWindow = 5.seconds

/** 녹음 시작 뒤 Next 가 켜지기까지의 시간(F2.4). */
val NextDelay = 10.seconds

data class ExamUiState(
    /** 지금 단계. 회차를 읽기 전에는 null 이다. */
    val step: ExamStep? = null,
    /** 서베이 단계의 화면 상태. 회차를 읽기 전에는 null 이다. */
    val survey: SurveyState? = null,
    val levelCount: Int = 0,
    val selectedLevel: Int? = null,
    val selectedRecalibration: Recalibration? = null,
    /** 채점 문항의 번호(1 부터)와 이번 회차 문항 수. 고른 수준에서 건너뛰는 문항은 세지 않는다. */
    val questionNumber: Int = 0,
    val questionCount: Int = 0,
    /** 남은 시험 시간. 1번 문항 전에는 null 이다. */
    val remaining: Duration? = null,
    /** 시험 전체 시간(40분). 남은 시간을 원으로 그릴 때 기준이다. 회차를 읽기 전에는 null 이다. */
    val timeLimit: Duration? = null,
    val replayUsed: Boolean = false,
    val phase: QuestionPhase = QuestionPhase.Ready,
    /** 지금 녹음이 시작된 뒤 흐른 시간. */
    val recorded: Duration = Duration.ZERO,
    /** 녹음 중 마이크 음량(0~1). 녹음 중이 아니면 0 이다. */
    val voiceLevel: Float = 0f,
    /** 문항 화면에 [recorded] 를 보일지(F2.7). 기본은 꺼짐이고 설정에서 켠다. */
    val answerTimer: Boolean = false,
    val error: ExamError? = null,
    /** 질문을 불러오는 중이거나 내장 문항으로 바꿨다는 안내. 단계가 바뀌면 지운다. */
    val questionNotice: QuestionNotice? = null,
    /** 누른 버튼(다음, Next, 나가기)의 저장이 도는 중이다. 끝날 때까지 그 버튼은 진행 표시를 보이고 다른 입력은 받지 않는다. */
    val saving: Boolean = false,
    val navigation: ExamDestination? = null,
    /** 마지막 문항까지 답하고 끝났다. 시간이 다 돼 끝난 것과 가른다. 끝 축하는 이때만 보인다. */
    val answeredAll: Boolean = false,
) {
    val canSubmitSurvey: Boolean get() = step is ExamStep.Survey && survey?.canSubmit == true
    val canReplay: Boolean get() = phase == QuestionPhase.Recording && !replayUsed && recorded < ReplayWindow && !saving
    val canNext: Boolean get() = phase == QuestionPhase.Recording && recorded >= NextDelay
}

/** 문항 화면의 진행. 질문 글자는 보이지 않고 읽기와 녹음만 있다(F2.3). */
enum class QuestionPhase { Ready, Speaking, Recording }

/** 문항 화면의 질문 안내. 기다리는 동안 무엇을 만드는지 보이고, 오래 걸리거나 내장 문항으로 바꾸면 알린다. */
sealed interface QuestionNotice {
    /** 모델이 [first]~[last] 번 질문을 만드는 중이다. [recalibrated] 면 재조정에서 고른 난이도에 맞추고, 아니면 서베이 주제로 만든다. */
    data class Making(val first: Int, val last: Int, val recalibrated: Boolean) : QuestionNotice

    /** [SlowNoticeDelay] 넘게 기다리고 있다. */
    data object Slow : QuestionNotice

    /** 모델 출제가 실패해서 내장 문항을 낸다(F6.2). */
    data object BuiltIn : QuestionNotice
}

enum class ExamError {
    /** 사용자가 마이크 권한을 주지 않았다. 설정에서 켜야 한다. */
    MicDenied,

    /** 권한은 있는데 마이크를 열지 못했다. 다른 앱이 마이크를 쓰고 있을 때 생긴다. */
    MicUnavailable,

    /** 녹음 조각 파일을 만들거나 쓰지 못했다. 저장 공간이 모자랄 때 생긴다. */
    RecordingFailed,

    /** 기기 음성으로 질문을 읽지 못했다. */
    SpeechFailed,

    /** 낼 질문을 얻지 못했다. */
    QuestionUnavailable,
}

internal fun RecordingFailure.toExamError(): ExamError = when (this) {
    RecordingFailure.MicUnavailable -> ExamError.MicUnavailable
    RecordingFailure.WriteFailed -> ExamError.RecordingFailed
}

/** 저장이 끝나야 가는 이동만 상태로 둔다(ADR 0009). 어디로 갈지는 화면을 등록한 쪽이 정한다. */
enum class ExamDestination {
    /** 회차를 남긴 채 나간다. 홈에서 이어 본다. */
    Exit,

    /** 마지막 문항을 냈거나 시간이 다 됐다. */
    Finished,
}

sealed interface ExamIntent {
    /** 지금 서베이 질문에서 [option] 보기를 골랐다. 바로 다음 화면으로 간다. */
    data class ChooseSurveyOption(val option: String) : ExamIntent

    data class ToggleTopic(val topic: String) : ExamIntent

    data object NextSurveyScreen : ExamIntent

    data object PreviousSurveyScreen : ExamIntent

    /** 서베이를 낸다. 묶음 최소와 전체 최소를 채웠을 때만 된다(F2.2). */
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

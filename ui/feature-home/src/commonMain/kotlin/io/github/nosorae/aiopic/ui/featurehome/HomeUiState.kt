package io.github.nosorae.aiopic.ui.featurehome

/** 모의고사 탭(S3). 이어 보기, 시작, 지난 회차 다시 보기. 이동은 모두 단순 이동이라 화면 람다로 한다(ADR 0009). */
data class HomeUiState(
    /** 진행 중인 회차 id. 있으면 이어 보기가 그 회차를 연다(F3). */
    val resumeSessionId: String? = null,
    /** 이어 볼 회차에서 답한 문항 수와 고른 수준의 전체 문항 수. 이어 보기 버튼 위 진행 점에 쓴다. */
    val resumeDone: Int = 0,
    val resumeTotal: Int = 0,
    /** 끝낸 회차, 최근 것부터. 다시 보기는 맨 앞 회차를 연다. 저장소를 읽기 전에는 null 이다. */
    val pastSessions: List<PastSession>? = null,
    /** 시작 전에 보이는 시험 진행 안내(#230). 시험 모양에서 읽고, 저장소를 읽기 전에는 null 이다. */
    val overview: ExamOverview? = null,
) {
    val canResume: Boolean get() = resumeSessionId != null

    /** 회차를 읽기 전에는 시작인지 이어 보기인지 몰라서 시험 버튼을 누를 수 없다. */
    val loading: Boolean get() = pastSessions == null
}

data class PastSession(
    val id: String,
    /** 처음 본 회차가 1 이다. */
    val number: Int,
    /** "IM2~IH" 처럼 보일 등급 범위. 분석이 끝나지 않았으면 null 이다. */
    val gradeRange: String?,
    /** 시간이 다 돼 끝났다(F2.6). */
    val timeUp: Boolean,
)

/** 시험이 어떻게 진행되는지. 화면은 시험 팩을 모르므로 시험 모양([io.github.nosorae.aiopic.engine.session.model.ExamSpec])에서 숫자만 옮겨 온다. */
data class ExamOverview(
    /** 서베이에서 골라야 하는 최소 주제 수. 서베이 단계가 없으면 0 이다. */
    val minTopics: Int,
    val levelCount: Int,
    /** 가장 낮은 수준과 가장 높은 수준에서 답하는 문항 수. */
    val minQuestions: Int,
    val maxQuestions: Int,
    val minutes: Int,
    /** 몇 번 문항을 마치면 난이도를 다시 고르는지. 재조정 단계가 없으면 null 이다. */
    val recalibrateAfter: Int?,
)

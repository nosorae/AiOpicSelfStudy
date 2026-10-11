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

package io.github.nosorae.aiopic.ui.featurehome

/** 홈(S3). 모의시험 시작과 이어 보기, 한 문항 풀기, 지난 회차. 이동은 모두 단순 이동이라 화면 람다로 한다(ADR 0009). */
data class HomeUiState(
    /** 진행 중인 회차가 있으면 시작 버튼이 이어 보기가 된다(F3). */
    val canResume: Boolean = false,
    /** 최근 것부터. 저장소를 읽기 전에는 null 이다. */
    val pastSessions: List<PastSession>? = null,
)

data class PastSession(
    val id: String,
    /** 처음 본 회차가 1 이다. */
    val number: Int,
    /** "IM2~IH" 처럼 보일 등급 범위. 분석이 끝나지 않았으면 null 이다. */
    val gradeRange: String?,
    /** 시간이 다 돼 끝났다(F2.6). */
    val timeUp: Boolean,
)

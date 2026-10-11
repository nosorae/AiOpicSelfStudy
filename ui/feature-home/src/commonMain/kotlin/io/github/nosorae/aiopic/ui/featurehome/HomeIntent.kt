package io.github.nosorae.aiopic.ui.featurehome

sealed interface HomeIntent {
    /** 확인 대화상자에서 새로 시작을 골랐다. 진행 중인 회차를 미완료로 끝낸다(#223). */
    data object StartNewExam : HomeIntent
}

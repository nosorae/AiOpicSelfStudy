package io.github.nosorae.aiopic.ui.featuresettings.practice

/**
 * 문항별 답변 타이머를 켤지 기기에 남긴다(F2.7). 기본은 꺼짐이다. 앱 층이 같은 값을 시험 화면에 넘긴다.
 * 실제 저장은 앱 층이 플랫폼 설정 저장소로 넣고, 테스트는 메모리 가짜를 쓴다.
 */
interface AnswerTimerStore {
    suspend fun answerTimerOn(): Boolean

    suspend fun setAnswerTimerOn(on: Boolean)
}

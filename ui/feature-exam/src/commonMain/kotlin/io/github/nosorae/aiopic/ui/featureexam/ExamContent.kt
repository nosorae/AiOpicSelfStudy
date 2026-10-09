package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.session.model.ExamSpec

/**
 * 시험 팩이 주는 시험 한 회의 모양과 서베이. 화면은 시험 팩을 모르고 앱 층이 이 값을 넣는다.
 * [surveyParts] 는 주제 고르기 앞에 나오는 Part 질문(오픽은 Part 1~3), [topics] 는 마지막 Part 의 관심 주제다.
 * [answerTimer] 는 설정의 문항별 답변 타이머(F2.7)다. 앱 층이 시험 화면을 열 때마다 설정 값을 읽어 넣는다.
 */
data class ExamContent(val spec: ExamSpec, val topics: List<String>, val surveyParts: List<SurveyPart>, val answerTimer: Boolean = false)

/** 서베이 Part 질문 하나와 고를 답. 하나만 고른다. */
data class SurveyPart(val question: String, val options: List<String>)

package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.question.model.Survey
import io.github.nosorae.aiopic.engine.session.model.ExamSpec

/**
 * 시험 팩이 주는 시험 한 회의 모양과 서베이. 화면은 시험 팩을 모르고 앱 층이 이 값을 넣는다.
 * [answerTimer] 는 설정의 문항별 답변 타이머(F2.7)다. 앱 층이 시험 화면을 열 때마다 설정 값을 읽어 넣는다.
 */
data class ExamContent(val spec: ExamSpec, val survey: Survey, val answerTimer: Boolean = false)

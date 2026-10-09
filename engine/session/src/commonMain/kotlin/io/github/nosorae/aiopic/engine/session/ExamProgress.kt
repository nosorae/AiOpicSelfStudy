package io.github.nosorae.aiopic.engine.session

import io.github.nosorae.aiopic.engine.session.model.ExamEvent
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import kotlin.time.Duration

/**
 * 지금 회차와 일어난 일로 다음 회차를 낸다(F3.1, ADR 0012). 저장도 화면도 건드리지 않아서 앱 꺼짐, 40분 끝, 7번 뒤 재조정을
 * 기기 없이 확인한다. 지금 단계에 맞지 않는 일과 끝난 회차에 온 일은 받은 회차를 그대로 돌려준다.
 *
 * 앱 꺼짐은 아무것도 바꾸지 않는다. 단계, 남은 시간, 다시 듣기 여부가 그대로 남아 다시 켜면 같은 곳에서 이어진다(F3.3).
 * Play 도 바꾸지 않는다. 질문 읽기와 녹음 시작은 화면 일이다.
 */
fun advance(spec: ExamSpec, session: ExamSession, event: ExamEvent): ExamSession {
    if (session.status != ExamStatus.InProgress) return session
    val step = spec.steps[session.stepIndex]
    return when (event) {
        is ExamEvent.SurveySubmitted ->
            if (step is ExamStep.Survey && event.topics.size >= step.minTopics) {
                session.copy(surveyTopics = event.topics).moveOn(spec)
            } else {
                session
            }

        is ExamEvent.LevelSelected ->
            if (step is ExamStep.LevelSelect && event.level in 1..spec.levelCount) {
                session.copy(level = event.level).moveOn(spec)
            } else {
                session
            }

        is ExamEvent.Recalibrated ->
            if (step is ExamStep.Recalibrate) session.copy(recalibration = event.choice).moveOn(spec) else session

        is ExamEvent.Next -> when (step) {
            is ExamStep.Check -> session.moveOn(spec)
            is ExamStep.Ask -> session.copy(answerIds = session.answerIds + listOfNotNull(event.answerId)).moveOn(spec)
            else -> session
        }

        ExamEvent.Replay -> if (step is ExamStep.Ask || step is ExamStep.Check) session.copy(replayUsed = true) else session

        is ExamEvent.TimePassed -> {
            val remaining = session.remaining ?: return session
            (remaining - event.duration).let { if (it > Duration.ZERO) session.copy(remaining = it) else session.timeUp() }
        }

        ExamEvent.TimeUp -> if (session.remaining == null) session else session.timeUp()

        ExamEvent.Play, ExamEvent.AppClosed -> session
    }
}

private fun ExamSession.timeUp() = copy(remaining = Duration.ZERO, status = ExamStatus.TimeUp)

/** 고른 수준에서 건너뛰는 문항을 빼고 다음 단계로 간다. 1번 문항에 처음 닿으면 40분을 켠다(F2.5). 남은 단계가 없으면 끝이다. */
private fun ExamSession.moveOn(spec: ExamSpec): ExamSession {
    val next = (stepIndex + 1 until spec.steps.size).firstOrNull { i ->
        val step = spec.steps[i]
        step !is ExamStep.Ask || step.minLevel <= (level ?: 1)
    } ?: return copy(replayUsed = false, status = ExamStatus.Finished)
    val startsTimer = remaining == null && spec.steps[next] is ExamStep.Ask
    return copy(stepIndex = next, replayUsed = false, remaining = if (startsTimer) spec.timeLimit else remaining)
}

package io.github.nosorae.aiopic.engine.session

import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamEvent
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.Recalibration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ExamProgressTest {
    private val ask = ExamStep.Ask(QuestionType("q"))
    private val askFrom3 = ExamStep.Ask(QuestionType("q"), minLevel = 3)

    /** 오픽과 같은 모양: 서베이, 수준, 점검, 1~7번, 재조정, 8~15번. 13~15번은 수준 3 부터. */
    private val spec = ExamSpec(
        timeLimit = 40.minutes,
        levelCount = 6,
        steps = listOf(ExamStep.Survey(minTopics = 2), ExamStep.LevelSelect, ExamStep.Check) +
            List(7) { ask } + ExamStep.Recalibrate + List(5) { ask } + List(3) { askFrom3 },
    )
    private val topics = listOf("home", "music")

    private fun ExamSession.on(vararg events: ExamEvent) = events.fold(this) { s, e -> advance(spec, s, e) }

    private fun step(session: ExamSession) = spec.steps[session.stepIndex]

    /** 서베이, 수준, 점검 연습 문항까지 지나 1번 문항에 선 회차. */
    private fun atFirstQuestion(level: Int = 5) = ExamSession(id = "s1").on(ExamEvent.SurveySubmitted(topics), ExamEvent.LevelSelected(level), ExamEvent.Next())

    private fun answer(n: Int) = Array(n) { ExamEvent.Next(answerId = "a$it") }

    /** Next 만 눌러 끝까지 가며 지난 문항 수를 센다. */
    private fun countQuestions(level: Int): Int {
        var session = atFirstQuestion(level)
        var count = 0
        while (session.status == ExamStatus.InProgress) {
            if (step(session) is ExamStep.Ask) count++
            session = session.on(if (step(session) is ExamStep.Recalibrate) ExamEvent.Recalibrated(Recalibration.Same) else ExamEvent.Next())
        }
        return count
    }

    @Test
    fun `F2_1 서베이 수준 점검을 지나면 1번 문항이다`() {
        val session = atFirstQuestion()

        assertEquals(3, session.stepIndex)
    }

    @Test
    fun `F2_1 서베이 주제가 최소 개수보다 적으면 다음으로 못 간다`() {
        val session = ExamSession(id = "s1").on(ExamEvent.SurveySubmitted(listOf("home")))

        assertEquals(0, session.stepIndex)
    }

    @Test
    fun `F2_1 고를 수 없는 수준이면 다음으로 못 간다`() {
        val session = ExamSession(id = "s1").on(ExamEvent.SurveySubmitted(topics), ExamEvent.LevelSelected(7))

        assertNull(session.level)
    }

    @Test
    fun `F2_1 7번에서 Next 를 누르면 재조정이다`() {
        val session = atFirstQuestion().on(*answer(7))

        assertIs<ExamStep.Recalibrate>(step(session))
    }

    @Test
    fun `F2_1 재조정을 고르면 저장하고 8번 문항으로 간다`() {
        val session = atFirstQuestion().on(*answer(7), ExamEvent.Recalibrated(Recalibration.Harder))

        assertEquals(Recalibration.Harder to 11, session.recalibration to session.stepIndex)
    }

    @Test
    fun `F2_1 재조정 화면에서 고르지 않고 Next 를 누르면 그대로다`() {
        val atRecalibrate = atFirstQuestion().on(*answer(7))

        assertEquals(atRecalibrate, atRecalibrate.on(ExamEvent.Next()))
    }

    @Test
    fun `F2_1 수준 1 과 2 는 12문항이다`() {
        assertEquals(listOf(12, 12), listOf(countQuestions(1), countQuestions(2)))
    }

    @Test
    fun `F2_1 수준 3 부터는 15문항이다`() {
        assertEquals(listOf(15, 15), listOf(countQuestions(3), countQuestions(6)))
    }

    @Test
    fun `F2_1 마지막 문항에서 Next 를 누르면 회차가 끝난다`() {
        val session = atFirstQuestion(level = 2).on(*answer(7), ExamEvent.Recalibrated(Recalibration.Same), *answer(5))

        assertEquals(ExamStatus.Finished, session.status)
    }

    @Test
    fun `답한 문항의 답변 id 가 순서대로 쌓이고 점검 연습 문항은 쌓이지 않는다`() {
        val session = ExamSession(id = "s1").on(
            ExamEvent.SurveySubmitted(topics),
            ExamEvent.LevelSelected(5),
            ExamEvent.Next(answerId = "practice"),
            *answer(2),
        )

        assertEquals(listOf("a0", "a1"), session.answerIds)
    }

    @Test
    fun `F2_4 다시 듣기는 한 문항에 한 번이고 다음 문항에서 다시 쓸 수 있다`() {
        val replayed = atFirstQuestion().on(ExamEvent.Play, ExamEvent.Replay)

        assertEquals(true to false, replayed.replayUsed to replayed.on(ExamEvent.Next()).replayUsed)
    }

    @Test
    fun `F2_4 어떤 일이 와도 지나간 문항으로 돌아가지 않는다`() {
        val session = atFirstQuestion().on(*answer(3))
        val events = listOf(
            ExamEvent.SurveySubmitted(topics),
            ExamEvent.LevelSelected(1),
            ExamEvent.Play,
            ExamEvent.Replay,
            ExamEvent.Next(),
            ExamEvent.Recalibrated(Recalibration.Easier),
            ExamEvent.AppClosed,
            ExamEvent.TimePassed(1.minutes),
        )

        assertTrue(events.all { advance(spec, session, it).stepIndex >= session.stepIndex })
    }

    @Test
    fun `F2_5 40분은 1번 문항에서 시작한다`() {
        val beforeFirst = ExamSession(id = "s1").on(ExamEvent.SurveySubmitted(topics), ExamEvent.LevelSelected(5))

        assertEquals(null to 40.minutes, beforeFirst.remaining to beforeFirst.on(ExamEvent.Next()).remaining)
    }

    @Test
    fun `F2_5 흐른 시간만큼 남은 시간이 준다`() {
        val session = atFirstQuestion().on(ExamEvent.TimePassed(90.seconds), ExamEvent.TimePassed(30.seconds))

        assertEquals(38.minutes, session.remaining)
    }

    @Test
    fun `F2_5 1번 전에 흐른 시간은 세지 않는다`() {
        val session = ExamSession(id = "s1").on(ExamEvent.TimePassed(5.minutes))

        assertNull(session.remaining)
    }

    @Test
    fun `F2_5 F3_1 앱이 꺼져도 같은 문항 같은 남은 시간 같은 다시 듣기 여부다`() {
        val running = atFirstQuestion().on(*answer(4), ExamEvent.Replay, ExamEvent.TimePassed(10.minutes))

        assertEquals(running, running.on(ExamEvent.AppClosed))
    }

    @Test
    fun `F3_1 7번 뒤에 앱이 꺼지면 다시 켰을 때 재조정부터다`() {
        val session = atFirstQuestion().on(*answer(7), ExamEvent.AppClosed)

        assertIs<ExamStep.Recalibrate>(step(session))
    }

    @Test
    fun `F2_6 40분이 다 흐르면 시간 끝이고 남은 시간은 0 이다`() {
        val session = atFirstQuestion().on(*answer(5), ExamEvent.TimePassed(41.minutes))

        assertEquals(ExamStatus.TimeUp to Duration.ZERO, session.status to session.remaining)
    }

    @Test
    fun `F2_6 시간 끝 알림을 받으면 시간 끝이다`() {
        val session = atFirstQuestion().on(ExamEvent.TimeUp)

        assertEquals(ExamStatus.TimeUp, session.status)
    }

    @Test
    fun `F2_6 시간이 끝나면 답한 문항까지만 남고 더 진행하지 않는다`() {
        val timeUp = atFirstQuestion().on(*answer(5), ExamEvent.TimeUp)

        val after = timeUp.on(ExamEvent.Next(answerId = "late"), ExamEvent.Replay, ExamEvent.TimePassed(1.minutes))

        assertEquals(timeUp, after)
    }

    @Test
    fun `F2_6 재조정 화면에서 시간이 끝나도 시간 끝이다`() {
        val session = atFirstQuestion().on(*answer(7), ExamEvent.TimePassed(40.minutes))

        assertEquals(ExamStatus.TimeUp, session.status)
    }

    @Test
    fun `끝난 회차는 어떤 일이 와도 그대로다`() {
        val finished = atFirstQuestion(level = 1).on(*answer(7), ExamEvent.Recalibrated(Recalibration.Easier), *answer(5))

        assertEquals(finished, finished.on(ExamEvent.TimeUp, ExamEvent.Next(), ExamEvent.SurveySubmitted(topics)))
    }

    @Test
    fun `문항 화면이 아닌 곳에서 온 다시 듣기는 무시한다`() {
        val atSurvey = ExamSession(id = "s1")

        assertFalse(atSurvey.on(ExamEvent.Replay).replayUsed)
    }
}

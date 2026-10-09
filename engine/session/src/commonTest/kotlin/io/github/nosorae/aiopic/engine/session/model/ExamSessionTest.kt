package io.github.nosorae.aiopic.engine.session.model

import io.github.nosorae.aiopic.engine.question.model.QuestionType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ExamSessionTest {
    private inline fun <reified T> roundTrip(value: T): T = Json.decodeFromString(Json.encodeToString(value))

    @Test
    fun `F2_1 단계 목록을 데이터로 둔 시험 명세는 저장했다 읽어도 같다`() {
        val spec = ExamSpec(
            timeLimit = 40.minutes,
            levelCount = 6,
            steps = listOf(
                ExamStep.Survey(minTopics = 12),
                ExamStep.LevelSelect,
                ExamStep.Check,
                ExamStep.Ask(QuestionType("self-intro")),
                ExamStep.Ask(QuestionType("comparison"), minLevel = 3),
                ExamStep.Recalibrate,
            ),
        )

        assertEquals(spec, roundTrip(spec))
    }

    @Test
    fun `F3_1 진행 중인 회차는 통째로 저장했다 읽어도 같다`() {
        val session = ExamSession(
            id = "s1",
            stepIndex = 4,
            surveyTopics = listOf("home", "music"),
            level = 5,
            recalibration = Recalibration.Harder,
            answerIds = listOf("a1", "a2"),
            remaining = 31.minutes,
            replayUsed = true,
            status = ExamStatus.InProgress,
            grade = null,
        )

        assertEquals(session, roundTrip(session))
    }

    @Test
    fun `F5_3 등급이 붙은 회차는 저장했다 읽어도 같다`() {
        val session = ExamSession(
            id = "s1",
            status = ExamStatus.Finished,
            grade = SessionGrade(
                checks = listOf(GradeCheck("C1", met = true), GradeCheck("C2", met = false)),
                low = "IH",
                high = "AL",
                reasons = listOf("Keeps paragraphs."),
                brokeAt = listOf(12),
            ),
        )

        assertEquals(session, roundTrip(session))
    }

    @Test
    fun `일어난 일은 저장했다 읽어도 같다`() {
        val events = listOf(
            ExamEvent.SurveySubmitted(listOf("home")),
            ExamEvent.LevelSelected(4),
            ExamEvent.Play,
            ExamEvent.Replay,
            ExamEvent.Next("a1"),
            ExamEvent.Recalibrated(Recalibration.Same),
            ExamEvent.AppClosed,
            ExamEvent.TimeUp,
            ExamEvent.TimePassed(3.seconds),
        )

        assertEquals(events, roundTrip<List<ExamEvent>>(events))
    }

    @Test
    fun `등급 체계는 저장했다 읽어도 같다`() {
        val scale = GradeScale(listOf("NL", "NM", "NH", "IL", "IM", "IH", "AL"))

        assertEquals(scale, roundTrip(scale))
    }

    @Test
    fun `등급 체계에 같은 등급이 둘이면 만들 수 없다`() {
        assertFailsWith<IllegalArgumentException> { GradeScale(listOf("IM", "IM")) }
    }
}

package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.question.model.Survey
import io.github.nosorae.aiopic.engine.question.model.SurveyAnswers
import io.github.nosorae.aiopic.engine.question.model.SurveyOption
import io.github.nosorae.aiopic.engine.question.model.SurveyQuestion
import io.github.nosorae.aiopic.engine.question.model.Topic
import io.github.nosorae.aiopic.engine.question.model.TopicGroup
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.libs.recorder.api.Recorder
import io.github.nosorae.aiopic.libs.recorder.api.RecordingFailure
import io.github.nosorae.aiopic.libs.recorder.api.StoppedRecording
import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** 일(회사원이면 근무 기간이 이어진다), 사는 곳, 여가 묶음과 취미 묶음(각 1개 이상), 합쳐서 2개 이상. */
val testSurvey = Survey(
    questions = listOf(
        SurveyQuestion("work", "하는 일", listOf(SurveyOption("office", "회사원", listOf(SurveyQuestion("period", "근무 기간", listOf(SurveyOption("short", "짧게"), SurveyOption("long", "오래"))))), SurveyOption("student", "학생"))),
        SurveyQuestion("home", "사는 곳", listOf(SurveyOption("alone", "혼자"), SurveyOption("family", "가족과"))),
    ),
    groups = listOf(
        TopicGroup("leisure", "여가", 1, listOf(Topic("영화", "영화"), Topic("공원", "공원"))),
        TopicGroup("hobbies", "취미", 1, listOf(Topic("요리", "요리"), Topic("여행", "여행"))),
    ),
    minTopics = 2,
)

/** 서베이(위 [testSurvey]), 수준(1~3), 점검, 1번, 재조정, 2번, 수준 3 이상만 3번. 40분. */
val testContent = ExamContent(
    spec = ExamSpec(
        timeLimit = 40.minutes,
        levelCount = 3,
        steps = listOf(
            ExamStep.Survey(minTopics = 2),
            ExamStep.LevelSelect,
            ExamStep.Check,
            ExamStep.Ask(QuestionType("intro")),
            ExamStep.Recalibrate,
            ExamStep.Ask(QuestionType("routine")),
            ExamStep.Ask(QuestionType("issue"), minLevel = 3),
        ),
    ),
    survey = testSurvey,
)

class FakeSurveyStore(var saved: SurveyAnswers? = null) : SurveyStore {
    override suspend fun surveyAnswers(): SurveyAnswers? = saved

    override suspend fun saveSurveyAnswers(answers: SurveyAnswers) {
        saved = answers
    }
}

/** [saveTime] 은 회차 저장 한 번에 걸리는 시간이다. 처리 중 표시를 볼 때 늘린다. */
class FakeExamRepository(var saveTime: Duration = Duration.ZERO) : ExamRepository {
    val sessions = mutableMapOf<String, ExamSession>()
    val answers = mutableListOf<Answer>()

    override suspend fun session(id: String): ExamSession? = sessions[id]

    override suspend fun save(session: ExamSession) {
        delay(saveTime)
        sessions[session.id] = session
    }

    /** 질문 하나를 받는 데 걸리는 시간과 그 질문이 내장 문항으로 바꾼 것인지. 불러오는 중 안내를 볼 때 바꾼다. */
    var questionTime: Duration = Duration.ZERO
    var fellBack = false

    override suspend fun question(session: ExamSession): ExamQuestion {
        delay(questionTime)
        return ExamQuestion(Question(id = "q${session.stepIndex}", text = "질문 ${session.stepIndex}", type = QuestionType("t"), topic = "영화"), fellBack)
    }

    override fun recordingDirectory(session: ExamSession): String = "rec/${session.id}/${session.stepIndex}"

    override suspend fun save(answer: Answer) {
        answers += answer
    }
}

/** 질문 하나를 [readTime] 동안 읽는다. [canSpeak] 가 false 면 읽지 못한다. */
class FakeTts(private val readTime: Duration = 3.seconds, var canSpeak: Boolean = true) : Tts {
    val spoken = mutableListOf<String>()

    override suspend fun speak(text: String): Boolean {
        if (!canSpeak) return false
        delay(readTime)
        spoken += text
        return true
    }

    override fun stop() = Unit
}

/** 파일을 쓰지 않는다. 시작할 때마다 조각 경로 하나를 더한다. [writeFails] 면 멈출 때 조각 쓰기에 실패했다고 알린다. */
class FakeRecorder(var micAvailable: Boolean = true, var writeFails: Boolean = false) : Recorder {
    private val folders = mutableMapOf<String, MutableList<String>>()
    var recording: String? = null

    override suspend fun start(directory: String): RecordingFailure? {
        if (!micAvailable) return RecordingFailure.MicUnavailable
        val chunks = folders.getOrPut(directory) { mutableListOf() }
        chunks += "$directory/${chunks.size}.m4a"
        recording = directory
        return null
    }

    override suspend fun stop(): StoppedRecording {
        val chunks = chunks(checkNotNull(recording) { "녹음 중이 아니다" }).also { recording = null }
        return StoppedRecording(chunks, if (writeFails) RecordingFailure.WriteFailed else null)
    }

    override suspend fun chunks(directory: String): List<String> = folders[directory].orEmpty().toList()

    override suspend fun merge(chunks: List<String>, output: String): List<Duration> = emptyList()
}

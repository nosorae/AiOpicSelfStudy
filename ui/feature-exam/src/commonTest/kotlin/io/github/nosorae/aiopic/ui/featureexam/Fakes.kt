package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.libs.recorder.api.Recorder
import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** 서베이(Part 1~2 와 주제 2개 이상), 수준(1~3), 점검, 1번, 재조정, 2번, 수준 3 이상만 3번. 40분. */
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
    topics = listOf("영화", "공원", "요리", "여행"),
    surveyParts = listOf(SurveyPart("하는 일", listOf("회사원", "학생")), SurveyPart("사는 곳", listOf("혼자", "가족과"))),
)

/** [saveTime] 은 회차 저장 한 번에 걸리는 시간이다. 처리 중 표시를 볼 때 늘린다. */
class FakeExamRepository(var lastTopics: List<String> = emptyList(), var saveTime: Duration = Duration.ZERO) : ExamRepository {
    val sessions = mutableMapOf<String, ExamSession>()
    val answers = mutableListOf<Answer>()

    override suspend fun session(id: String): ExamSession? = sessions[id]

    override suspend fun save(session: ExamSession) {
        delay(saveTime)
        sessions[session.id] = session
    }

    override suspend fun lastSurveyTopics(): List<String> = lastTopics

    override suspend fun question(session: ExamSession): Question = Question(id = "q${session.stepIndex}", text = "질문 ${session.stepIndex}", type = QuestionType("t"), topic = "영화")

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

/** 파일을 쓰지 않는다. 시작할 때마다 조각 경로 하나를 더한다. */
class FakeRecorder(var micAvailable: Boolean = true) : Recorder {
    private val folders = mutableMapOf<String, MutableList<String>>()
    var recording: String? = null

    override suspend fun start(directory: String): Boolean {
        if (!micAvailable) return false
        val chunks = folders.getOrPut(directory) { mutableListOf() }
        chunks += "$directory/${chunks.size}.m4a"
        recording = directory
        return true
    }

    override suspend fun stop(): List<String> = chunks(checkNotNull(recording) { "녹음 중이 아니다" }).also { recording = null }

    override suspend fun chunks(directory: String): List<String> = folders[directory].orEmpty().toList()

    override suspend fun merge(chunks: List<String>, output: String): List<Duration> = emptyList()
}

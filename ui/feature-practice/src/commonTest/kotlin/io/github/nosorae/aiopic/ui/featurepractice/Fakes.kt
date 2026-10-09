package io.github.nosorae.aiopic.ui.featurepractice

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.libs.recorder.api.Recorder
import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun question(id: String, type: String) = Question(id = id, text = "질문 $id", type = QuestionType(type), topic = "영화")

/** [question] 에 답했고 분석이 [score] 점으로 끝난 답변. 교정 하나가 붙어 있다. */
fun scored(id: String, question: Question, score: Int) = Answer(
    id = id,
    question = question,
    examSessionId = null,
    chunkPaths = emptyList(),
    analysis = AnswerAnalysis.Done(
        analyzedBy = AnalyzedBy("p", "m", "v1"),
        feedback = Feedback(
            transcript = "I go to park",
            corrections = listOf(Correction("go to park", "go to the park")),
            upgrades = emptyList(),
            tenseConsistent = true,
            sentenceCount = 1,
            wordsPerMinute = 100.0,
            pauses = emptyList(),
            roleplayQuestionCount = null,
            pronunciation = Pronunciation.NotEvaluated,
            score = score,
        ),
    ),
)

class FakePracticeRepository(
    var pool: List<Question> = listOf(question("q1", "routine")),
    val answers: MutableList<Answer> = mutableListOf(),
    var flags: Set<CorrectionFlag> = emptySet(),
) : PracticeRepository {
    override suspend fun pool(): List<Question> = pool

    override suspend fun answers(): List<Answer> = answers.toList()

    override suspend fun flaggedCorrections(): Set<CorrectionFlag> = flags

    override fun recordingDirectory(answerId: String): String = "rec/$answerId"

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

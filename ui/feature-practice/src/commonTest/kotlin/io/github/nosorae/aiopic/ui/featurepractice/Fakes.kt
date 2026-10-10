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
import io.github.nosorae.aiopic.libs.recorder.api.RecordingFailure
import io.github.nosorae.aiopic.libs.recorder.api.StoppedRecording
import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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

/** [saveTime] 은 답변 저장 한 번에 걸리는 시간이다. 처리 중 표시를 볼 때 늘린다. */
class FakePracticeRepository(
    var pool: List<Question> = listOf(question("q1", "routine")),
    val answers: MutableList<Answer> = mutableListOf(),
    var flags: Set<CorrectionFlag> = emptySet(),
    var saveTime: Duration = Duration.ZERO,
) : PracticeRepository {
    override suspend fun pool(): List<Question> = pool

    override suspend fun answers(): List<Answer> = answers.toList()

    override suspend fun flaggedCorrections(): Set<CorrectionFlag> = flags

    override fun recordingDirectory(answerId: String): String = "rec/$answerId"

    override suspend fun save(answer: Answer) {
        delay(saveTime)
        answers += answer
    }
}

/** 질문 하나를 [readTime] 동안 읽는다. [canSpeak] 가 false 면 읽지 못한다. */
class FakeTts(private val readTime: Duration = 3.seconds, var canSpeak: Boolean = true) : Tts {
    val spoken = mutableListOf<String>()

    override suspend fun speak(text: String, audio: ByteArray?): Boolean {
        if (!canSpeak) return false
        delay(readTime)
        spoken += text
        return true
    }

    override fun hasHighQualityVoice() = true

    override fun stop() = Unit
}

/**
 * 파일을 쓰지 않는다. 시작할 때마다 조각 경로 하나를 더한다. [writeFails] 면 멈출 때 조각 쓰기에 실패했다고 알린다.
 * 음량은 테스트가 [level] 에 넣은 값을 그대로 낸다.
 */
class FakeRecorder(var micAvailable: Boolean = true, var writeFails: Boolean = false) : Recorder {
    private val folders = mutableMapOf<String, MutableList<String>>()
    var recording: String? = null
    val level = MutableStateFlow(0f)
    override val levels = level

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

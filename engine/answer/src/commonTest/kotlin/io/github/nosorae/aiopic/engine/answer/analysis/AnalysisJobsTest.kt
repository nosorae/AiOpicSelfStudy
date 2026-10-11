package io.github.nosorae.aiopic.engine.answer.analysis

import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.JobKind
import io.github.nosorae.aiopic.engine.answer.model.JobState
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.AudioAnswerAnalyzer
import io.github.nosorae.aiopic.engine.answer.port.FakeDeafProvider
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.port.SavedResponses
import io.github.nosorae.aiopic.engine.answer.port.SessionGrader
import io.github.nosorae.aiopic.engine.answer.port.TextAnalysis
import io.github.nosorae.aiopic.engine.answer.port.Transcriber
import io.github.nosorae.aiopic.engine.answer.port.Transcript
import io.github.nosorae.aiopic.engine.answer.port.TranscriptAnalyzer
import io.github.nosorae.aiopic.engine.answer.route.AnswerRouter
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.answer.store.inMemoryDatabase
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.GradeCheck
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * 분석 작업 표(#205)가 어떤 경우에도 성공이나 실패로 끝나는지 본다. 시간은 테스트 시계로 돌려서 재시도 간격과 시간 제한을 바로 넘긴다.
 * 실패 경로마다 Gemini 호출 수를 세어 받은 응답을 잃거나 같은 일을 다시 부르지 않는지도 본다.
 */
class AnalysisJobsTest {
    private val dispatcher = StandardTestDispatcher()
    private val db = inMemoryDatabase(dispatcher)
    private val store = OpicStore(db)

    @AfterTest
    fun close() = db.close()

    private val model = ModelInfo(provider = "fake-hearing", id = "hear-1", displayName = "Hear", hearsAudio = true, stability = Stability.Stable)
    private val feedback = Feedback(
        transcript = "I live in an apartment.",
        corrections = emptyList(),
        upgrades = emptyList(),
        tenseConsistent = true,
        sentenceCount = 1,
        wordsPerMinute = 150.0,
        pauses = emptyList(),
        roleplayQuestionCount = null,
        pronunciation = Pronunciation.Evaluated("Clear."),
        score = 80,
    )
    private val grade = SessionGrade(listOf(GradeCheck("C1", true)), low = "IM2", high = "IH", reasons = listOf("C1 met."), brokeAt = listOf(15))

    /** 답변 분석 결과. 함수라서 부를 때마다 다른 결과를 줄 수 있다. */
    private var answerResult: suspend (Question) -> AnalysisResult<Feedback> = { AnalysisResult.Success(feedback) }
    private var analyzed = 0
    private val analyzer = object : AudioAnswerAnalyzer {
        override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, recording: Recording): AnalysisResult<Feedback> {
            analyzed++
            return answerResult(question)
        }
    }

    private var gradeResult: AnalysisResult<SessionGrade> = AnalysisResult.Success(grade)
    private val graded = mutableListOf<Int>()
    private val grader = object : SessionGrader {
        override suspend fun grade(model: ModelInfo, prompt: Prompt, scale: GradeScale, recordings: List<Recording>): AnalysisResult<SessionGrade> {
            graded += recordings.size
            return gradeResult
        }
    }

    private var consented = true
    private var wakes = 0
    private val recording = Recording(ByteArray(0), "audio/mp4", emptyList())

    private fun TestScope.jobs(router: AnswerRouter = AnswerRouter(analyzer, FakeDeafProvider(), FakeDeafProvider()), request: suspend (Answer) -> AnalysisRequest = { AnalysisRequest(model, Prompt("answer-v1", "analyze", "{}"), recording) }) = AnalysisJobs(
        store = store,
        router = router,
        grader = grader,
        consented = { consented },
        answerRequest = request,
        gradeRequest = { answers -> GradeRequest(model, Prompt("grade-v1", "grade", "{}"), GradeScale(listOf("IM2", "IH")), answers.map { recording }) },
        wake = { wakes++ },
        clock = object : Clock {
            override fun now() = Instant.fromEpochMilliseconds(testScheduler.currentTime)
        },
    )

    private fun answer(id: String = "a1", sessionId: String? = null, question: String = "q1") = Answer(id, Question(question, "Tell me.", QuestionType("description"), "home"), sessionId, listOf("$id/0.m4a"))

    private suspend fun saveSession(count: Int = 15, status: ExamStatus = ExamStatus.Finished) {
        val ids = (1..count).map { "a$it" }
        ids.forEachIndexed { i, id -> store.save(answer(id, "s1", "q${i + 1}")) }
        store.save(ExamSession("s1", answerIds = ids, status = status))
    }

    private suspend fun job(id: String = "a1", kind: JobKind = JobKind.Answer): AnalysisJob? = store.jobs().first().firstOrNull { it.kind == kind && it.targetId == id }

    private suspend fun analysis(id: String = "a1") = store.answers().first().first { it.id == id }.analysis

    private suspend fun session() = store.session("s1").first()!!

    @Test
    fun `205 넣은 답변은 대기로 남고 깨우면 분석해 성공으로 끝난다`() = runTest(dispatcher) {
        val jobs = jobs()
        store.save(answer())

        jobs.enqueueAnswer("a1")

        assertEquals(JobState.Queued, job()?.state)
        assertEquals(1, wakes)
        assertNull(jobs.drain())
        assertEquals(JobState.Succeeded, job()?.state)
        assertEquals(AnswerAnalysis.Done::class, analysis()::class)
    }

    @Test
    fun `F5_6 429 와 5xx 는 30초부터 두 배씩 기다려 3번 다시 보내고 다 쓰면 실패로 끝난다`() = runTest(dispatcher) {
        val jobs = jobs()
        store.save(answer())
        jobs.enqueueAnswer("a1")
        answerResult = { AnalysisResult.Failed(AnalysisError.Retryable) }

        val waits = mutableListOf<Duration?>()
        repeat(4) {
            waits += jobs.drain()
            if (it < 3) {
                assertEquals(JobState.RetryScheduled, job()?.state)
                // 다시 보내기를 기다리는 동안 답변은 기다림이다. 화면은 작업 표의 다음 시도 시각을 보인다.
                assertEquals(AnswerAnalysis.Pending, analysis())
            }
            waits.last()?.let { advanceTimeBy(it) }
        }

        assertEquals(listOf(30.seconds, 60.seconds, 120.seconds, null), waits)
        assertEquals(4, analyzed)
        assertEquals(JobState.Failed, job()?.state)
        assertEquals(AnalysisError.Retryable, job()?.error)
        assertEquals(AnswerAnalysis.Failed(AnalysisError.Retryable), analysis())
    }

    @Test
    fun `F5_6 재시도 시각 전에는 다시 보내지 않는다`() = runTest(dispatcher) {
        val jobs = jobs()
        store.save(answer())
        jobs.enqueueAnswer("a1")
        answerResult = { AnalysisResult.Failed(AnalysisError.Retryable) }
        jobs.drain()

        advanceTimeBy(29.seconds)
        assertEquals(1.seconds, jobs.drain())

        assertEquals(1, analyzed)
        assertEquals(Instant.fromEpochMilliseconds(30_000), job()?.nextAttemptAt)
    }

    @Test
    fun `205 오프라인은 횟수를 쓰지 않고 네트워크 기다림으로 두었다가 연결되면 끝낸다`() = runTest(dispatcher) {
        val jobs = jobs()
        store.save(answer())
        jobs.enqueueAnswer("a1")
        answerResult = { AnalysisResult.Failed(AnalysisError.Offline) }

        repeat(6) {
            assertEquals(30.seconds, jobs.drain())
            assertEquals(JobState.WaitingNetwork, job()?.state)
        }
        assertEquals(0, job()?.attempts)

        answerResult = { AnalysisResult.Success(feedback) }
        assertNull(jobs.drain())
        assertEquals(JobState.Succeeded, job()?.state)
    }

    @Test
    fun `205 오프라인이면 그 차례에서 멈추고 뒤 작업을 보내지 않는다`() = runTest(dispatcher) {
        val jobs = jobs()
        store.save(answer("a1"))
        store.save(answer("a2"))
        jobs.enqueueAnswer("a1")
        jobs.enqueueAnswer("a2")
        answerResult = { AnalysisResult.Failed(AnalysisError.Offline) }

        jobs.drain()

        assertEquals(1, analyzed)
        assertEquals(JobState.Queued, job("a2")?.state)
    }

    @Test
    fun `205 Gemini 가 시간 제한 안에 답하지 않으면 다시 보낼 실패로 센다`() = runTest(dispatcher) {
        val jobs = jobs()
        store.save(answer())
        jobs.enqueueAnswer("a1")
        answerResult = { awaitCancellation() }

        assertEquals(30.seconds, jobs.drain())

        assertEquals(JobState.RetryScheduled, job()?.state)
        assertEquals(1, job()?.attempts)
        assertEquals(2.minutes.inWholeMilliseconds, testScheduler.currentTime)
    }

    @Test
    fun `205 키 틀림 모델 없음 응답 이상은 다시 보내지 않고 이유를 남겨 실패로 끝낸다`() = runTest(dispatcher) {
        val outcomes = listOf(AnalysisError.InvalidKey, AnalysisError.ModelUnavailable, AnalysisError.Rejected).associateWith { error ->
            val id = "a-$error"
            val jobs = jobs()
            store.save(answer(id))
            jobs.enqueueAnswer(id)
            answerResult = { AnalysisResult.Failed(error) }
            jobs.drain()
            job(id)!!.state to analysis(id)
        }

        outcomes.forEach { (error, outcome) -> assertEquals(JobState.Failed to AnswerAnalysis.Failed(error), outcome) }
        assertEquals(3, analyzed)
    }

    @Test
    fun `205 요청을 만들다 예외가 나도 실패로 끝내고 다음 작업으로 넘어간다`() = runTest(dispatcher) {
        val jobs = jobs(request = { if (it.id == "a1") error("녹음 파일이 없다") else AnalysisRequest(model, Prompt("answer-v1", "analyze", "{}"), recording) })
        store.save(answer("a1"))
        store.save(answer("a2"))
        jobs.enqueueAnswer("a1")
        jobs.enqueueAnswer("a2")

        jobs.drain()

        assertEquals(AnswerAnalysis.Failed(AnalysisError.Rejected), analysis("a1"))
        assertEquals(JobState.Succeeded, job("a2")?.state)
    }

    @Test
    fun `205 앱이 진행 중에 죽으면 켤 때 대기로 되돌려 다시 처리한다`() = runTest(dispatcher) {
        store.save(answer())
        jobs().enqueueAnswer("a1")
        // 프로세스가 죽으면 코드가 더 돌지 않아 진행 중이 그대로 남는다.
        store.save(job()!!.copy(state = JobState.Running))

        val restarted = jobs()
        restarted.recover()

        assertEquals(JobState.Queued, job()?.state)
        restarted.drain()
        assertEquals(JobState.Succeeded, job()?.state)
    }

    @Test
    fun `205 보내는 중에 플랫폼이 멈추면 프로세스가 살아 있으니 바로 대기로 돌린다`() = runTest(dispatcher) {
        val jobs = jobs()
        store.save(answer())
        jobs.enqueueAnswer("a1")
        answerResult = { awaitCancellation() }
        val drain = backgroundScope.launch { jobs.drain() }
        runCurrent()
        assertEquals(JobState.Running, job()?.state)

        drain.cancel()
        runCurrent()

        assertEquals(JobState.Queued, job()?.state)
    }

    @Test
    fun `205 앱을 켜면 작업 표에 없는 기다림 답변을 대기로 옮긴다`() = runTest(dispatcher) {
        // 작업 표가 없던 버전에서 쌓인 답변(DB 이전)과, 답변을 저장한 뒤 작업을 넣기 전에 앱이 죽은 경우다.
        store.save(answer("a1"))
        store.save(answer("a2").copy(analysis = AnswerAnalysis.Failed(AnalysisError.Rejected)))

        jobs().recover()

        assertEquals(listOf("a1"), store.jobs().first().map { it.targetId })
        assertEquals(JobState.Queued, job("a1")?.state)
    }

    @Test
    fun `205 이미 결과가 있는 답변은 다시 부르지 않고 성공으로 끝낸다`() = runTest(dispatcher) {
        val jobs = jobs()
        store.save(answer())
        jobs.enqueueAnswer("a1")
        jobs.drain()
        store.save(store.jobs().first().single().copy(state = JobState.Queued))

        jobs.drain()

        assertEquals(1, analyzed)
        assertEquals(JobState.Succeeded, job()?.state)
    }

    @Test
    fun `205 지워진 답변의 작업은 부르지 않고 성공으로 끝낸다`() = runTest(dispatcher) {
        val jobs = jobs()
        jobs.enqueueAnswer("none")

        jobs.drain()

        assertEquals(0, analyzed)
        assertEquals(JobState.Succeeded, job("none")?.state)
    }

    @Test
    fun `205 같은 답변을 두 번 넣어도 한 번만 부른다`() = runTest(dispatcher) {
        val jobs = jobs()
        store.save(answer())
        jobs.enqueueAnswer("a1")
        jobs.enqueueAnswer("a1")

        jobs.drain()

        assertEquals(1, analyzed)
        assertEquals(1, store.jobs().first().size)
    }

    /** 실제 분석기처럼 받은 응답 원문을 먼저 저장하고 그다음 해석한다. [parse] 가 예외를 내면 해석 실패다. */
    private var parse: (String) -> AnalysisResult<Feedback> = { AnalysisResult.Success(feedback) }
    private var called = 0
    private val savingAnalyzer = object : AudioAnswerAnalyzer {
        override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, recording: Recording): AnalysisResult<Feedback> {
            val saved = currentCoroutineContext()[SavedResponses]!!
            val raw = saved.take("answer") ?: "raw-${++called}".also { saved.save("answer", it) }
            return answerResult(question).takeIf { it !is AnalysisResult.Success } ?: parse(raw)
        }
    }

    @Test
    fun `205 응답을 받은 직후 앱이 죽어도 다시 켜면 저장한 원문으로 끝내고 다시 부르지 않는다`() = runTest(dispatcher) {
        store.save(answer())
        answerResult = { awaitCancellation() }
        val dead = jobs(AnswerRouter(savingAnalyzer, FakeDeafProvider(), FakeDeafProvider()))
        dead.enqueueAnswer("a1")
        val process = backgroundScope.launch { dead.drain() }
        runCurrent()
        process.cancel()
        assertEquals(1, called)

        answerResult = { AnalysisResult.Success(feedback) }
        val restarted = jobs(AnswerRouter(savingAnalyzer, FakeDeafProvider(), FakeDeafProvider()))
        restarted.recover()
        restarted.drain()

        assertEquals(1, called)
        assertEquals(AnswerAnalysis.Done::class, analysis()::class)
    }

    @Test
    fun `205 해석에 실패해도 원문이 남아 다시 분석하기는 다시 부르지 않고 다시 해석만 한다`() = runTest(dispatcher) {
        store.save(answer())
        val jobs = jobs(AnswerRouter(savingAnalyzer, FakeDeafProvider(), FakeDeafProvider()))
        jobs.enqueueAnswer("a1")
        parse = { error("해석 코드가 이 응답을 못 읽는다") }
        jobs.drain()
        assertEquals(AnswerAnalysis.Failed(AnalysisError.Rejected), analysis())

        parse = { AnalysisResult.Success(feedback.copy(transcript = it)) }
        jobs.retryAnswer("a1")
        jobs.drain()

        assertEquals(1, called)
        assertEquals("raw-1", (analysis() as AnswerAnalysis.Done).feedback.transcript)
    }

    @Test
    fun `205 두 단계 분석은 두 번째 단계가 실패해 다시 보내도 받아쓰기를 다시 부르지 않는다`() = runTest(dispatcher) {
        var transcribed = 0
        var textResult: AnalysisResult<TextAnalysis> = AnalysisResult.Failed(AnalysisError.Retryable)
        val deaf = FakeDeafProvider()
        val transcriber = object : Transcriber {
            override suspend fun transcribe(recording: Recording): AnalysisResult<Transcript> {
                transcribed++
                return AnalysisResult.Success(deaf.transcript)
            }
        }
        val text = object : TranscriptAnalyzer {
            override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, transcript: Transcript) = textResult.also { assertEquals(deaf.transcript, transcript) }
        }
        val jobs = jobs(AnswerRouter(analyzer, transcriber, text), request = { AnalysisRequest(deaf.defaultModel, Prompt("answer-v1", "analyze", "{}"), recording) })
        store.save(answer())
        jobs.enqueueAnswer("a1")

        advanceTimeBy(jobs.drain()!!)
        textResult = AnalysisResult.Success(deaf.analysis)
        jobs.drain()

        assertEquals(1, transcribed)
        assertEquals(AnswerAnalysis.Done::class, analysis()::class)
    }

    @Test
    fun `F5_3 마지막 답변 분석이 끝난 뒤에만 등급 작업이 한 번 들어가고 등급을 회차에 남긴다`() = runTest(dispatcher) {
        saveSession()
        val jobs = jobs()
        answerResult = { if (it.id == "q15") AnalysisResult.Unscorable(UnscorableReason.TooShort) else AnalysisResult.Success(feedback) }
        (1..14).forEach { jobs.enqueueAnswer("a$it") }
        jobs.drain()
        assertNull(job("s1", JobKind.Grade))

        jobs.enqueueAnswer("a15")
        jobs.drain()

        assertEquals(grade, session().grade)
        assertEquals(listOf(15), graded)
        assertEquals(JobState.Succeeded, job("s1", JobKind.Grade)?.state)

        jobs.requestGradeIfReady("s1")
        jobs.drain()
        assertEquals(listOf(15), graded)
    }

    @Test
    fun `F2_6 시간이 다 돼 끝난 회차는 답한 문항만 다 분석되면 회차를 저장할 때 등급을 요청한다`() = runTest(dispatcher) {
        saveSession(count = 12, status = ExamStatus.InProgress)
        val jobs = jobs()
        (1..12).forEach { jobs.enqueueAnswer("a$it") }
        jobs.drain()
        assertNull(job("s1", JobKind.Grade))

        store.save(session().copy(status = ExamStatus.TimeUp))
        jobs.requestGradeIfReady("s1")

        assertEquals(JobState.Queued, job("s1", JobKind.Grade)?.state)
    }

    @Test
    fun `205 등급이 평가할 수 없다고 오면 기다림으로 남기지 않고 실패로 끝낸다`() = runTest(dispatcher) {
        saveSession(count = 1)
        val jobs = jobs()
        gradeResult = AnalysisResult.Unscorable(UnscorableReason.Silent)
        jobs.enqueueAnswer("a1")

        jobs.drain()

        assertEquals(JobState.Failed, job("s1", JobKind.Grade)?.state)
        assertEquals(AnalysisError.Rejected, session().gradeError)
    }

    @Test
    fun `205 등급이 다시 보내기를 다 쓰면 회차에 이유를 남긴다`() = runTest(dispatcher) {
        saveSession(count = 1)
        val jobs = jobs()
        gradeResult = AnalysisResult.Failed(AnalysisError.Retryable)
        jobs.enqueueAnswer("a1")

        var wait = jobs.drain()
        while (wait != null) {
            assertNull(session().gradeError)
            advanceTimeBy(wait)
            wait = jobs.drain()
        }

        assertEquals(4, graded.size)
        assertEquals(AnalysisError.Retryable, session().gradeError)
    }

    @Test
    fun `203 분석하지 못한 답변이 있으면 등급을 요청하지 않는다`() = runTest(dispatcher) {
        saveSession(count = 2)
        val jobs = jobs()
        answerResult = { if (it.id == "q2") AnalysisResult.Failed(AnalysisError.Rejected) else AnalysisResult.Success(feedback) }
        jobs.enqueueAnswer("a1")
        jobs.enqueueAnswer("a2")

        jobs.drain()

        assertNull(job("s1", JobKind.Grade))
    }

    @Test
    fun `203 다시 분석하기는 실패한 답변과 등급을 대기로 돌리고 다 끝나면 등급을 다시 낸다`() = runTest(dispatcher) {
        saveSession(count = 2)
        val jobs = jobs()
        answerResult = { if (it.id == "q2") AnalysisResult.Failed(AnalysisError.Rejected) else AnalysisResult.Success(feedback) }
        jobs.enqueueAnswer("a1")
        jobs.enqueueAnswer("a2")
        jobs.drain()

        answerResult = { AnalysisResult.Success(feedback) }
        jobs.retrySession("s1")
        assertEquals(AnswerAnalysis.Pending, analysis("a2"))
        assertEquals(JobState.Queued, job("a2")?.state)
        jobs.drain()

        assertEquals(grade, session().grade)
    }

    @Test
    fun `203 등급만 실패한 회차는 다시 분석하기로 이유를 지우고 등급을 다시 낸다`() = runTest(dispatcher) {
        saveSession(count = 1)
        val jobs = jobs()
        gradeResult = AnalysisResult.Failed(AnalysisError.InvalidKey)
        jobs.enqueueAnswer("a1")
        jobs.drain()
        assertEquals(AnalysisError.InvalidKey, session().gradeError)

        gradeResult = AnalysisResult.Success(grade)
        jobs.retrySession("s1")
        assertNull(session().gradeError)
        jobs.drain()

        assertEquals(grade, session().grade)
    }

    @Test
    fun `F1_3 동의가 없으면 보내지 않고 동의 없음으로 실패한다`() = runTest(dispatcher) {
        val jobs = jobs()
        store.save(answer())
        jobs.enqueueAnswer("a1")
        consented = false

        jobs.drain()

        assertEquals(0, analyzed)
        assertEquals(AnswerAnalysis.Failed(AnalysisError.NoConsent), analysis())
    }

    @Test
    fun `F1_3 동의를 철회하면 남은 작업을 바로 동의 없음 실패로 끝낸다`() = runTest(dispatcher) {
        val jobs = jobs()
        store.save(answer("a1"))
        store.save(answer("a2"))
        jobs.enqueueAnswer("a1")
        jobs.drain()
        jobs.enqueueAnswer("a2")

        jobs.cancelAll()

        assertEquals(JobState.Succeeded, job("a1")?.state)
        assertEquals(JobState.Failed, job("a2")?.state)
        assertEquals(AnswerAnalysis.Failed(AnalysisError.NoConsent), analysis("a2"))
    }
}

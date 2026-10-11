package io.github.nosorae.aiopic.engine.answer.analysis

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.AudioAnswerAnalyzer
import io.github.nosorae.aiopic.engine.answer.port.FakeDeafProvider
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.port.SessionGrader
import io.github.nosorae.aiopic.engine.answer.route.AnswerRouter
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.answer.store.inMemoryDatabase
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.GradeCheck
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import io.github.nosorae.aiopic.libs.workqueue.api.WorkQueue
import io.github.nosorae.aiopic.libs.workqueue.api.WorkResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionGradeHandlerTest {
    private val db = inMemoryDatabase()
    private val store = OpicStore(db)

    @AfterTest
    fun close() = db.close()

    private val queue = object : WorkQueue {
        val ids = mutableListOf<String>()

        override suspend fun enqueue(id: String, payload: String) {
            if (id !in ids) ids += id
        }

        override suspend fun cancelAll() = ids.clear()
    }

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

    /** 마지막 답변만 평가할 수 없게 돌려준다. Done 과 Unscorable 이 섞여도 등급을 요청하는지 본다. */
    private val analyzer = object : AudioAnswerAnalyzer {
        override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, recording: Recording): AnalysisResult<Feedback> = if (question.id == "q15") AnalysisResult.Unscorable(UnscorableReason.TooShort) else AnalysisResult.Success(feedback)
    }

    private var gradeResult: AnalysisResult<SessionGrade> = AnalysisResult.Success(grade)
    private val graded = mutableListOf<Int>()
    private val grader = object : SessionGrader {
        override suspend fun grade(model: ModelInfo, prompt: Prompt, scale: GradeScale, recordings: List<Recording>): AnalysisResult<SessionGrade> {
            graded += recordings.size
            return gradeResult
        }
    }

    private fun handler(consented: Boolean = true): SessionGradeHandler {
        val deaf = FakeDeafProvider()
        val recording = { _: Answer -> Recording(ByteArray(0), "audio/mp4", emptyList()) }
        return SessionGradeHandler(
            store = store,
            queue = queue,
            answers = AnswerAnalysisHandler(store, AnswerRouter(analyzer, deaf, deaf), { consented }) { AnalysisRequest(model, Prompt("answer-v1", "analyze", "{}"), recording(it)) },
            grader = grader,
            consented = { consented },
            request = { answers -> GradeRequest(model, Prompt("grade-v1", "grade", "{}"), GradeScale(listOf("IM2", "IH")), answers.map(recording)) },
        )
    }

    private suspend fun saveSession(count: Int = 15, status: ExamStatus = ExamStatus.Finished, grade: SessionGrade? = null) {
        val ids = (1..count).map { "a$it" }
        ids.forEachIndexed { i, id -> store.save(Answer(id, Question("q${i + 1}", "Tell me.", QuestionType("description"), "home"), "s1", listOf("$id/0.m4a"))) }
        store.save(ExamSession("s1", answerIds = ids, status = status, grade = grade))
    }

    private suspend fun session() = store.session("s1").first()!!

    @Test
    fun `F5_3 마지막 답변 분석이 저장된 뒤에만 등급 요청이 한 번 들어간다`() = runTest {
        saveSession()
        val handler = handler()

        (1..14).forEach { handler.handle("a$it") }
        assertEquals(emptyList(), queue.ids)

        handler.handle("a15")
        handler.handle("a15")

        assertEquals(listOf(gradePayload("s1")), queue.ids)
        assertEquals(AnswerAnalysis.Unscorable::class, store.answers("s1").first().last().analysis::class)
    }

    @Test
    fun `F2_6 시간이 다 돼 끝난 회차는 답한 문항만 다 분석되면 회차를 저장할 때 등급을 요청한다`() = runTest {
        saveSession(count = 12, status = ExamStatus.InProgress)
        val handler = handler()
        (1..12).forEach { handler.handle("a$it") }
        assertEquals(emptyList(), queue.ids)

        store.save(session().copy(status = ExamStatus.TimeUp))
        requestGradeIfReady(store, queue, "s1")

        assertEquals(listOf(gradePayload("s1")), queue.ids)
    }

    @Test
    fun `F5_3 등급을 받으면 회차에 남기고 이미 등급이 있으면 다시 보내지 않는다`() = runTest {
        saveSession()
        val handler = handler()
        (1..15).forEach { handler.handle("a$it") }

        assertEquals(WorkResult.Success, handler.handle(gradePayload("s1")))
        assertEquals(grade, session().grade)
        assertEquals(listOf(15), graded)

        assertEquals(WorkResult.Success, handler.handle(gradePayload("s1")))
        requestGradeIfReady(store, queue, "s1")
        assertEquals(listOf(15), graded)
    }

    @Test
    fun `F5_6 429 408 5xx 오프라인만 다시 보내고 나머지 실패는 버린다`() = runTest {
        saveSession()
        val handler = handler()
        (1..15).forEach { handler.handle("a$it") }

        val saved = mutableListOf<AnalysisError?>()
        val outcomes = AnalysisError.entries.associateWith {
            gradeResult = AnalysisResult.Failed(it)
            handler.handle(gradePayload("s1")).also { saved += session().gradeError }
        }

        assertEquals(
            mapOf(
                AnalysisError.Retryable to WorkResult.Retry,
                AnalysisError.InvalidKey to WorkResult.Failure,
                AnalysisError.ModelUnavailable to WorkResult.Failure,
                AnalysisError.Rejected to WorkResult.Failure,
            ),
            outcomes,
        )
        assertNull(session().grade)
        // 회차에 이유가 남아야 리포트가 키 오류와 연결 오류를 다르게 안내한다(#203).
        assertEquals<List<AnalysisError?>>(AnalysisError.entries, saved)
    }

    @Test
    fun `203 분석하지 못한 답변이 있으면 등급을 요청하지 않는다`() = runTest {
        saveSession()
        val handler = handler()
        (1..14).forEach { handler.handle("a$it") }
        store.save(store.answers("s1").first().last().copy(analysis = AnswerAnalysis.Failed(AnalysisError.Rejected)))

        requestGradeIfReady(store, queue, "s1")

        assertEquals(emptyList(), queue.ids)
    }

    @Test
    fun `203 다시 분석하기는 실패한 답변과 등급을 기다림으로 돌려 대기열에 다시 넣는다`() = runTest {
        saveSession()
        val handler = handler()
        (1..15).forEach { handler.handle("a$it") }
        gradeResult = AnalysisResult.Failed(AnalysisError.Rejected)
        handler.handle(gradePayload("s1"))
        store.save(store.answers("s1").first().first().copy(analysis = AnswerAnalysis.Failed(AnalysisError.Retryable)))
        queue.ids.clear()

        retrySession(store, queue, "s1")

        assertEquals(AnswerAnalysis.Pending, store.answers("s1").first().first().analysis)
        assertNull(session().gradeError)
        assertEquals(listOf("a1"), queue.ids)

        gradeResult = AnalysisResult.Success(grade)
        handler.handle("a1")
        assertEquals(listOf("a1", gradePayload("s1")), queue.ids)
    }

    @Test
    fun `203 앱을 켜면 대기열에서 빠진 기다림을 다시 넣는다`() = runTest {
        saveSession(count = 2)
        store.save(store.answers("s1").first().last().copy(analysis = AnswerAnalysis.Failed(AnalysisError.Rejected)))

        requeuePending(store, queue)

        assertEquals(listOf("a1"), queue.ids)
    }

    @Test
    fun `F1_3 음성 전송 동의를 철회하면 등급 요청을 보내지 않는다`() = runTest {
        saveSession()
        (1..15).forEach { handler().handle("a$it") }

        assertEquals(WorkResult.Failure, handler(consented = false).handle(gradePayload("s1")))

        assertEquals(emptyList(), graded)
        assertNull(session().grade)
    }
}

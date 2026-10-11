package io.github.nosorae.aiopic.engine.answer.store

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.GradeCheck
import io.github.nosorae.aiopic.engine.session.model.Recalibration
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 플랫폼마다 메모리 DB 를 만든다. Android 는 호스트 JVM, iOS 는 시뮬레이터에서 같은 테스트를 돈다.
 * [context] 에 테스트 디스패처를 주면 쿼리가 테스트 시계 위에서 돈다. 쿼리가 다른 스레드에서 도는 동안 테스트 시계가 시간 제한을 넘겨 버리지 않게 한다.
 */
expect fun inMemoryDatabase(context: CoroutineContext? = null): OpicDatabase

class OpicStoreTest {
    private val db = inMemoryDatabase()
    private val store = OpicStore(db)

    @AfterTest
    fun close() = db.close()

    private val session = ExamSession(
        id = "s1",
        stepIndex = 9,
        surveyTopics = listOf("home", "music"),
        level = 5,
        recalibration = Recalibration.Harder,
        answerIds = listOf("a1", "a2"),
        remaining = 27.minutes + 13.seconds,
        replayUsed = true,
    )

    private fun answer(id: String, examSessionId: String?) = Answer(
        id = id,
        question = Question(id = "q-$id", text = "Tell me about your house.", type = QuestionType("description"), topic = "home"),
        examSessionId = examSessionId,
        chunkPaths = listOf("$id/0.m4a", "$id/1.m4a"),
        resumedAt = listOf(5.seconds),
    )

    @Test
    fun `F3_1 회차 상태를 저장하고 다시 열면 그대로 복원된다`() = runTest {
        store.save(session)

        assertEquals(session, store.session("s1").first())
    }

    @Test
    fun `F3_1 상태가 바뀌면 통째로 덮어쓰고 회차는 하나로 남는다`() = runTest {
        store.save(session)
        val finished = session.copy(
            stepIndex = 16,
            remaining = 2.minutes,
            status = ExamStatus.Finished,
            grade = SessionGrade(listOf(GradeCheck("c1", met = true)), low = "IM2", high = "IH", reasons = listOf("r"), brokeAt = listOf(12)),
        )

        store.save(finished)

        assertEquals(finished, store.session("s1").first())
        assertEquals(listOf(finished), store.sessions().first())
    }

    @Test
    fun `저장한 적 없는 회차는 null 이다`() = runTest {
        assertNull(store.session("none").first())
    }

    @Test
    fun `F7 회차의 답변은 저장한 순서대로 나오고 한 문항 연습 답변은 섞이지 않는다`() = runTest {
        val a1 = answer("a1", "s1")
        val practice = answer("p1", null)
        val a2 = answer("a2", "s1")
        listOf(a1, practice, a2).forEach { store.save(it) }

        assertEquals(listOf(a1, a2), store.answers("s1").first())
        assertEquals(listOf(a1, practice, a2), store.answers().first())
    }

    @Test
    fun `F5_7 분석 결과로 답변을 덮어써도 순서와 모델 정보가 남는다`() = runTest {
        val a1 = answer("a1", "s1")
        val a2 = answer("a2", "s1")
        store.save(a1)
        store.save(a2)
        val scored = a1.copy(
            analysis = AnswerAnalysis.Unscorable(AnalyzedBy("gemini", "test-model", "answer-v1"), UnscorableReason.TooShort),
        )

        store.save(scored)

        assertEquals(listOf(scored, a2), store.answers("s1").first())
    }

    @Test
    fun `F8_1 틀림 표시를 저장하고 해제하면 남은 표시만 나온다`() = runTest {
        store.mark(CorrectionFlag("a1", 0), wrong = true)
        store.mark(CorrectionFlag("a1", 2), wrong = true)
        store.mark(CorrectionFlag("a1", 0), wrong = true)

        assertEquals(setOf(CorrectionFlag("a1", 0), CorrectionFlag("a1", 2)), store.flags().first())

        store.mark(CorrectionFlag("a1", 0), wrong = false)

        assertEquals(setOf(CorrectionFlag("a1", 2)), store.flags().first())
    }
}

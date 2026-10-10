package io.github.nosorae.aiopic.apps.opic.shared.data

import io.github.nosorae.aiopic.apps.opic.shared.AppSettings
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.store.OpicDatabase
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.libs.workqueue.api.WorkQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/** 플랫폼마다 메모리 DB 를 만든다. Android 는 호스트 JVM, iOS 는 시뮬레이터에서 같은 테스트를 돈다. */
expect fun inMemoryDatabase(): OpicDatabase

/** 넣은 id 를 순서대로 모은다. 같은 id 는 한 번만 넣는 대기열 규칙을 따른다. */
class FakeWorkQueue : WorkQueue {
    val ids = mutableListOf<String>()

    override suspend fun enqueue(id: String, payload: String) {
        if (id !in ids) ids += id
    }

    override suspend fun cancelAll() = ids.clear()
}

private val spec = ExamSpec(
    timeLimit = 40.minutes,
    levelCount = 3,
    steps = listOf(
        ExamStep.Survey(minTopics = 1),
        ExamStep.LevelSelect,
        ExamStep.Check,
        ExamStep.Ask(QuestionType("intro")),
        ExamStep.Recalibrate,
        ExamStep.Ask(QuestionType("routine")),
        ExamStep.Ask(QuestionType("issue"), minLevel = 3),
    ),
)

private val pool = listOf("intro", "routine", "issue").flatMap { type ->
    listOf("영화", "공원", "요리").map { topic -> Question("$type-$topic", "$type $topic", QuestionType(type), topic) }
}

private fun answer(id: String, sessionId: String?) = Answer(id, pool.first(), sessionId, chunkPaths = listOf("$id/0.m4a"))

class StoreRepositoriesTest {
    private val db = inMemoryDatabase()
    private val store = OpicStore(db)
    private val queue = FakeWorkQueue()
    private val reads = StoreReads(store)

    private val settings = AppSettings.inMemory()

    private fun exam() = StoreExamRepository(store, queue, spec, { pool }, recordingsRoot = "root", settings, maxOutsideTopics = 0, CoroutineScope(EmptyCoroutineContext)) { _, _, _ -> null }

    private val practice = StorePracticeRepository(store, queue, { pool }, recordingsRoot = "root")

    @AfterTest
    fun close() = db.close()

    @Test
    fun `F3_1 시험 저장소에 저장한 회차를 다시 읽는다`() = runTest {
        val session = ExamSession("s1", stepIndex = 3, surveyTopics = listOf("영화"), level = 2)

        exam().save(session)

        assertEquals(session, exam().session("s1"))
        assertNull(exam().session("none"))
    }

    @Test
    fun `F2_2 가장 최근에 서베이를 고른 회차의 주제를 돌려준다`() = runTest {
        assertEquals(emptyList(), exam().lastSurveyTopics())
        exam().save(ExamSession("s1", surveyTopics = listOf("영화")))
        exam().save(ExamSession("s2", surveyTopics = listOf("공원", "요리")))
        exam().save(ExamSession("s3"))

        assertEquals(listOf("공원", "요리"), exam().lastSurveyTopics())
    }

    @Test
    fun `F3_3 같은 단계는 저장소를 다시 만들어도 같은 질문이고 단계의 유형을 따른다`() = runTest {
        val session = ExamSession("s1", surveyTopics = listOf("영화", "요리"), level = 3)
        val questions = (0..6).map { exam().question(session.copy(stepIndex = it))?.question }

        assertEquals(questions, (0..6).map { exam().question(session.copy(stepIndex = it))?.question })
        assertEquals(listOf(null, null, "intro", "intro", null, "routine", "issue"), questions.map { it?.type?.id })
        assertEquals(4, questions.filterNotNull().map { it.id }.toSet().size)
    }

    @Test
    fun `수준을 고르기 전이나 수준이 모자란 단계에는 낼 질문이 없다`() = runTest {
        assertNull(exam().question(ExamSession("s1", stepIndex = 3)))
        assertNull(exam().question(ExamSession("s1", stepIndex = 6, level = 2)))
    }

    @Test
    fun `F3_3 같은 단계는 같은 녹음 폴더이고 단계와 회차가 다르면 폴더도 다르다`() {
        val session = ExamSession("s1", stepIndex = 3)
        val folder = exam().recordingDirectory(session)

        assertEquals(folder, exam().recordingDirectory(session.copy(replayUsed = true, remaining = 1.minutes)))
        assertNotEquals(folder, exam().recordingDirectory(session.copy(stepIndex = 5)))
        assertNotEquals(folder, exam().recordingDirectory(session.copy(id = "s2")))
        assertTrue(folder.startsWith("root/"))
    }

    @Test
    fun `F5_1 답변을 저장하면 저장소에 남고 분석 대기열에 들어간다`() = runTest {
        exam().save(answer("s1-3", "s1"))
        practice.save(answer("p1", null))

        assertEquals(listOf("s1-3", "p1"), store.answers().first().map { it.id })
        assertEquals(listOf("s1-3", "p1"), queue.ids)
    }

    @Test
    fun `연습 저장소는 문항 풀과 답변과 틀림 표시를 읽고 답변마다 녹음 폴더가 다르다`() = runTest {
        val saved = answer("p1", null)
        practice.save(saved)
        reads.mark(CorrectionFlag("p1", 0), wrong = true)

        assertEquals(pool, practice.pool())
        assertEquals(listOf(saved), practice.answers())
        assertEquals(setOf(CorrectionFlag("p1", 0)), practice.flaggedCorrections())
        assertTrue(practice.recordingDirectory("p1").startsWith("root/"))
        assertNotEquals(practice.recordingDirectory("p1"), practice.recordingDirectory("p2"))
    }

    @Test
    fun `F7 리포트와 기록과 홈은 저장한 회차와 답변을 저장한 순서로 읽는다`() = runTest {
        val s1 = ExamSession("s1", level = 2)
        val s2 = ExamSession("s2", level = 3)
        val a1 = answer("s1-3", "s1")
        val p1 = answer("p1", null)
        listOf(s1, s2).forEach { store.save(it) }
        listOf(a1, p1).forEach { store.save(it) }

        assertEquals(s1, reads.session("s1").first())
        assertEquals(listOf(a1), reads.answers("s1").first())
        assertEquals(listOf(a1, p1), reads.answers().first())
        assertEquals(listOf(s1, s2), reads.sessions().first())
    }

    @Test
    fun `F8_1 F8_3 틀림 표시를 저장하고 해제하면 기록과 내보내기가 같은 표시를 읽는다`() = runTest {
        reads.mark(CorrectionFlag("a1", 0), wrong = true)
        reads.mark(CorrectionFlag("a1", 1), wrong = true)
        reads.mark(CorrectionFlag("a1", 0), wrong = false)

        assertEquals(setOf(CorrectionFlag("a1", 1)), reads.flags().first())
    }
}

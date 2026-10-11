package io.github.nosorae.aiopic.engine.answer.store

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.github.nosorae.aiopic.engine.answer.analysis.AnalysisJobs
import io.github.nosorae.aiopic.engine.answer.analysis.AnalysisRequest
import io.github.nosorae.aiopic.engine.answer.analysis.GradeRequest
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.JobState
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.AudioAnswerAnalyzer
import io.github.nosorae.aiopic.engine.answer.port.FakeDeafProvider
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.port.SessionGrader
import io.github.nosorae.aiopic.engine.answer.route.AnswerRouter
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 작업 표가 없던 DB(version 1)를 연 사용자 기기에서 기다리던 답변이 작업 표로 옮겨져 다시 분석되는지 본다(#205).
 * version 1 의 표는 그때 Room 이 만든 SQL 그대로 쓴다.
 */
class MigrationTest {
    private val path = NSTemporaryDirectory() + "migration-test.db"

    @AfterTest
    fun delete() {
        listOf("", "-wal", "-shm").forEach { NSFileManager.defaultManager.removeItemAtPath(path + it, null) }
    }

    private val feedback = Feedback("I live in Seoul.", emptyList(), emptyList(), true, 1, 120.0, emptyList(), null, Pronunciation.Evaluated("Clear."), 80)

    private fun answer(id: String, analysis: AnswerAnalysis = AnswerAnalysis.Pending) = Answer(id, Question("q1", "Tell me.", QuestionType("description"), "home"), null, listOf("$id/0.m4a"), analysis = analysis)

    private fun writeVersion1(vararg answers: Answer) {
        val connection = BundledSQLiteDriver().open(path)
        listOf(
            "CREATE TABLE IF NOT EXISTS `exam_session` (`id` TEXT NOT NULL, `json` TEXT NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `answer` (`id` TEXT NOT NULL, `examSessionId` TEXT, `json` TEXT NOT NULL, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_answer_examSessionId` ON `answer` (`examSessionId`)",
            "CREATE TABLE IF NOT EXISTS `correction_flag` (`answerId` TEXT NOT NULL, `index` INTEGER NOT NULL, PRIMARY KEY(`answerId`, `index`))",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '6e28c0b46918633f87acfa7ca2679f66')",
            "PRAGMA user_version = 1",
        ).forEach(connection::execSQL)
        answers.forEach { connection.execSQL("INSERT INTO answer (id, examSessionId, json) VALUES ('${it.id}', NULL, '${Json.encodeToString(it)}')") }
        connection.close()
    }

    @Test
    fun `205 version 1 에서 기다리던 답변은 작업 표로 옮겨져 다시 분석된다`() = runTest {
        writeVersion1(answer("a1"), answer("a2", AnswerAnalysis.Failed(AnalysisError.Rejected)))
        val db = Room.databaseBuilder<OpicDatabase>(name = path).setDriver(BundledSQLiteDriver()).addMigrations(*OPIC_MIGRATIONS).build()
        val store = OpicStore(db)
        val deaf = FakeDeafProvider()
        val jobs = AnalysisJobs(
            store = store,
            router = AnswerRouter(
                object : AudioAnswerAnalyzer {
                    override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, recording: Recording) = AnalysisResult.Success(feedback)
                },
                deaf,
                deaf,
            ),
            grader = object : SessionGrader {
                override suspend fun grade(model: ModelInfo, prompt: Prompt, scale: GradeScale, recordings: List<Recording>) = AnalysisResult.Failed(AnalysisError.Rejected)
            },
            consented = { true },
            answerRequest = { AnalysisRequest(deaf.defaultModel, Prompt("v1", "analyze", "{}"), Recording(ByteArray(0), "audio/mp4", emptyList())) },
            gradeRequest = { GradeRequest(deaf.defaultModel, Prompt("v1", "grade", "{}"), GradeScale(emptyList()), emptyList()) },
            wake = {},
            clock = object : Clock {
                override fun now() = Instant.fromEpochMilliseconds(0)
            },
        )

        jobs.recover()
        assertEquals(listOf("a1" to JobState.Queued), store.jobs().first().map { it.targetId to it.state })
        jobs.drain()

        assertEquals(JobState.Succeeded, store.jobs().first().single().state)
        assertEquals(AnswerAnalysis.Done::class, store.answers().first().first { it.id == "a1" }.analysis::class)
        db.close()
    }
}

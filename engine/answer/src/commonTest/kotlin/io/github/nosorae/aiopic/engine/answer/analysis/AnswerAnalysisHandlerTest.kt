package io.github.nosorae.aiopic.engine.answer.analysis

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.answer.port.AnalysisError
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.AudioAnswerAnalyzer
import io.github.nosorae.aiopic.engine.answer.port.FakeDeafProvider
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.route.AnswerRouter
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.answer.store.inMemoryDatabase
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.libs.workqueue.api.WorkResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AnswerAnalysisHandlerTest {
    private val db = inMemoryDatabase()
    private val store = OpicStore(db)

    @AfterTest
    fun close() = db.close()

    private val answer = Answer(
        id = "a1",
        question = Question(id = "q1", text = "Tell me about your house.", type = QuestionType("description"), topic = "home"),
        examSessionId = "s1",
        chunkPaths = listOf("a1/0.m4a"),
    )
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

    private var result: AnalysisResult<Feedback> = AnalysisResult.Success(feedback)
    private var calls = 0
    private val analyzer = object : AudioAnswerAnalyzer {
        override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, recording: Recording): AnalysisResult<Feedback> {
            calls++
            return result
        }
    }

    private fun handler(consented: Boolean = true): AnswerAnalysisHandler {
        val deaf = FakeDeafProvider()
        return AnswerAnalysisHandler(
            store = store,
            router = AnswerRouter(analyzer, deaf, deaf),
            consented = { consented },
            request = { AnalysisRequest(model, Prompt("answer-v1", "analyze", "{}"), Recording(ByteArray(0), "audio/mp4", it.resumedAt)) },
        )
    }

    private suspend fun stored() = store.answers().first().single()

    @Test
    fun `F5_1 대기열이 꺼낸 답변을 분석해 결과를 저장한다`() = runTest {
        store.save(answer)

        assertEquals(WorkResult.Success, handler().handle("a1"))

        assertIs<AnswerAnalysis.Done>(stored().analysis)
    }

    @Test
    fun `F1_3 음성 전송 동의가 없으면 보내지 않고 답변은 분석 전으로 남는다`() = runTest {
        store.save(answer)

        assertEquals(WorkResult.Failure, handler(consented = false).handle("a1"))

        assertEquals(0, calls)
        assertEquals(AnswerAnalysis.Pending, stored().analysis)
    }

    @Test
    fun `F5_6 429 408 5xx 오프라인만 다시 보내고 나머지 실패는 버린다`() = runTest {
        store.save(answer)
        val outcomes = AnalysisError.entries.associateWith {
            result = AnalysisResult.Failed(it)
            handler().handle("a1")
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
    }

    @Test
    fun `203 분석이 실패하면 이유를 답변에 남겨 화면이 끝없이 기다리지 않는다`() = runTest {
        store.save(answer)
        val stored = AnalysisError.entries.associateWith {
            result = AnalysisResult.Failed(it)
            handler().handle("a1")
            stored().analysis
        }

        assertEquals(AnalysisError.entries.associateWith { AnswerAnalysis.Failed(it) }, stored)
    }

    @Test
    fun `203 실패로 남은 답변도 대기열이 다시 꺼내면 다시 보낸다`() = runTest {
        store.save(answer.copy(analysis = AnswerAnalysis.Failed(AnalysisError.Retryable)))

        assertEquals(WorkResult.Success, handler().handle("a1"))

        assertIs<AnswerAnalysis.Done>(stored().analysis)
    }

    @Test
    fun `이미 분석했거나 지워진 답변은 다시 보내지 않는다`() = runTest {
        store.save(answer.copy(analysis = AnswerAnalysis.Done(AnalyzedBy("p", "m", "v"), feedback)))

        assertEquals(WorkResult.Success, handler().handle("a1"))
        assertEquals(WorkResult.Success, handler().handle("none"))
        assertEquals(0, calls)
    }
}

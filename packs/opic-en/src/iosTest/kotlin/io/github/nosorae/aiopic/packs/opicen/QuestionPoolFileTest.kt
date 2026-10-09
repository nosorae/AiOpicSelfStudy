package io.github.nosorae.aiopic.packs.opicen

import io.github.nosorae.aiopic.engine.question.pick.parseQuestions
import io.github.nosorae.aiopic.engine.question.pick.pickFromPool
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** 내장 문항 풀(F6.2)을 Res 로 읽어 본다. Compose 리소스를 읽어야 해서 [OpicPromptFileTest] 처럼 iOS 시뮬레이터에서만 돈다. */
class QuestionPoolFileTest {
    private suspend fun pool() = parseQuestions(Res.readBytes("files/questions/pool.json").decodeToString())

    private val fullOrder = OpicEn.examSpec.steps.filterIsInstance<ExamStep.Ask>().map { it.questionType }

    @Test
    fun `F6_2 내장 풀은 문항 유형마다 문항이 있다`() = runTest {
        assertEquals(OpicQuestionType.entries.map { it.type }.toSet(), pool().map { it.type }.toSet())
    }

    @Test
    fun `F6_2 서베이 주제가 무엇이든 내장 풀로 15문항을 채운다`() = runTest {
        val pool = pool()
        val surveys = listOf(
            pool.map { it.topic }.distinct().filter { it != "self" }.take(12),
            (1..12).map { "unknown-$it" },
        )
        surveys.forEach { topics ->
            repeat(20) { seed ->
                val picked = assertNotNull(pickFromPool(pool, fullOrder, topics, Random(seed)), "$topics $seed")
                assertEquals(fullOrder, picked.map { it.type })
            }
        }
    }
}

package io.github.nosorae.aiopic.engine.question.pick

import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestionPickTest {
    private val intro = QuestionType("intro")
    private val describe = QuestionType("describe")
    private val routine = QuestionType("routine")
    private val roleplay = QuestionType("roleplay")

    private val order = listOf(intro, describe, routine, describe, routine, roleplay)
    private val topics = listOf("home", "music", "park")

    private fun q(id: String, type: QuestionType, topic: String) = Question(id, "text $id", type, topic)

    private val pool = listOf(
        q("i1", intro, "self"),
        q("d-home", describe, "home"),
        q("r-home", routine, "home"),
        q("d-music", describe, "music"),
        q("r-music", routine, "music"),
        q("d-park", describe, "park"),
        q("r-park", routine, "park"),
        q("d-gym", describe, "gym"),
        q("r-gym", routine, "gym"),
        q("rp-park", roleplay, "park"),
    )

    private val generated = listOf(
        q("g1", intro, "self"),
        q("g2", describe, "home"),
        q("g3", routine, "home"),
        q("g4", describe, "music"),
        q("g5", routine, "music"),
        q("g6", roleplay, "phone-shop"),
    )

    @Test
    fun `F6_1 생성한 질문이 유형 순서와 주제 검사를 통과하면 그대로 쓴다`() = runTest {
        val picked = pickQuestions(order, topics, pool, maxOutsideTopics = 2) { generated }

        assertEquals(generated, picked)
    }

    @Test
    fun `F6_2 생성이 실패하면 내장 풀에서 서베이 주제로 고른다`() = runTest {
        val picked = pickQuestions(order, topics, pool, maxOutsideTopics = 2, random = Random(1)) { null }!!

        assertEquals(order, picked.map { it.type })
        assertTrue(picked.drop(1).all { it.topic in topics }, "$picked")
        assertEquals(picked.size, picked.map { it.id }.toSet().size, "같은 문항을 두 번 내지 않는다")
    }

    @Test
    fun `F6_2 유형 순서가 어긋난 생성 결과는 버리고 내장 풀로 간다`() = runTest {
        val swapped = listOf(generated[0], generated[2], generated[1]) + generated.drop(3)

        val picked = pickQuestions(order, topics, pool, maxOutsideTopics = 2) { swapped }!!

        assertEquals(order, picked.map { it.type })
        assertTrue(picked.none { it.id.startsWith("g") })
    }

    @Test
    fun `F6_2 서베이 밖 주제가 너무 많은 생성 결과는 버리고 내장 풀로 간다`() = runTest {
        val offTopic = generated.map { it.copy(topic = "${it.topic}-x") }

        val picked = pickQuestions(order, topics, pool, maxOutsideTopics = 2) { offTopic }!!

        assertTrue(picked.none { it.id.startsWith("g") })
    }

    @Test
    fun `생성 결과 검사는 문항 수와 유형과 빈 글과 서베이 밖 주제 수를 본다`() {
        assertEquals(emptyList(), checkQuestions(generated, order, topics, maxOutsideTopics = 2))
        assertEquals(1, checkQuestions(generated.dropLast(1), order, topics, maxOutsideTopics = 2).size)
        assertEquals(1, checkQuestions(generated.map { it.copy(text = " ") }.take(1) + generated.drop(1), order, topics, 2).size)
        assertEquals(1, checkQuestions(generated, order, topics, maxOutsideTopics = 1).size, "self 와 phone-shop 둘이 서베이 밖이다")
    }

    @Test
    fun `내장 풀은 한 세트를 같은 주제로 묶는다`() {
        val picked = pickFromPool(pool, order, topics, Random(7))!!

        assertEquals(picked[1].topic, picked[2].topic)
        assertEquals(picked[3].topic, picked[4].topic)
        assertTrue(picked[1].topic != picked[3].topic)
    }

    @Test
    fun `서베이 주제에 없는 유형은 다른 주제 문항으로 채운다`() {
        val picked = pickFromPool(pool, listOf(describe, describe, describe, describe), listOf("home"), Random(3))!!

        assertEquals("home", picked.first().topic)
        assertEquals(4, picked.map { it.id }.toSet().size)
    }

    @Test
    fun `풀에 남은 문항이 없는 유형이 있으면 null 이다`() {
        assertNull(pickFromPool(pool, listOf(roleplay, roleplay), topics, Random(0)))
        assertNull(pickFromPool(pool, listOf(QuestionType("comparison")), topics, Random(0)))
    }

    @Test
    fun `F6_3 문항 파일은 질문 JSON 목록이다`() {
        val text = """[{"id": "my-1", "text": "Tell me about your favorite cafe.", "type": "describe", "topic": "cafe"}]"""

        assertEquals(listOf(Question("my-1", "Tell me about your favorite cafe.", describe, "cafe")), parseQuestions(text))
    }

    @Test
    fun `F6_3 빈 목록과 빈 칸과 겹친 id 와 깨진 JSON 은 거절한다`() {
        val one = """{"id": "a", "text": "t", "type": "describe", "topic": "cafe"}"""
        listOf(
            "[]",
            """[{"id": "a", "text": "", "type": "describe", "topic": "cafe"}]""",
            "[$one, $one]",
            "[$one",
            """{"questions": []}""",
        ).forEach { assertFailsWith<IllegalArgumentException>(it) { parseQuestions(it) } }
    }
}

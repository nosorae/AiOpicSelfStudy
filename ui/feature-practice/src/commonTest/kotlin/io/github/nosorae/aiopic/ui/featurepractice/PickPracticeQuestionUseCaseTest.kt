package io.github.nosorae.aiopic.ui.featurepractice

import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PickPracticeQuestionUseCaseTest {
    private val routine = question("r1", "routine")
    private val roleplay = question("p1", "roleplay")
    private val roleplay2 = question("p2", "roleplay")

    @Test
    fun `약점 점수가 가장 낮은 유형에서 낸다`() = runTest {
        val repository = FakePracticeRepository(
            pool = listOf(routine, roleplay),
            answers = mutableListOf(scored("a1", routine, 80), scored("a2", roleplay, 40)),
        )

        assertEquals("roleplay", PickPracticeQuestionUseCase(repository)()?.type?.id)
    }

    @Test
    fun `틀림 표시한 답변은 약점 점수에서 빠진다`() = runTest {
        val repository = FakePracticeRepository(
            pool = listOf(routine, roleplay),
            answers = mutableListOf(scored("a1", routine, 80), scored("a2", roleplay, 40)),
            flags = setOf(CorrectionFlag("a2", 0)),
        )

        assertEquals("routine", PickPracticeQuestionUseCase(repository)()?.type?.id)
    }

    @Test
    fun `약한 유형 안에서는 아직 답하지 않은 질문을 먼저 낸다`() = runTest {
        val repository = FakePracticeRepository(
            pool = listOf(routine, roleplay, roleplay2),
            answers = mutableListOf(scored("a1", routine, 80), scored("a2", roleplay, 40)),
        )

        assertEquals("p2", PickPracticeQuestionUseCase(repository)()?.id)
    }

    @Test
    fun `가장 약한 유형의 문항이 풀에 없으면 그다음 약한 유형에서 낸다`() = runTest {
        val repository = FakePracticeRepository(
            pool = listOf(routine),
            answers = mutableListOf(scored("a1", routine, 80), scored("a2", roleplay, 40)),
        )

        assertEquals("r1", PickPracticeQuestionUseCase(repository)()?.id)
    }

    @Test
    fun `분석된 답변이 없으면 풀 전체에서 낸다`() = runTest {
        assertEquals("r1", PickPracticeQuestionUseCase(FakePracticeRepository(pool = listOf(routine)))()?.id)
    }

    @Test
    fun `풀이 비었으면 null 이다`() = runTest {
        assertNull(PickPracticeQuestionUseCase(FakePracticeRepository(pool = emptyList()))())
    }
}

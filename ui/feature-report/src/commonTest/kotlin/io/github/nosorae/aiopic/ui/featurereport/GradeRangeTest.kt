package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import kotlin.test.Test
import kotlin.test.assertEquals

class GradeRangeTest {
    @Test
    fun `F5_3 등급 범위는 낮은 등급과 높은 등급을 함께 쓴다`() {
        assertEquals("IH ~ AL", SessionGrade(emptyList(), "IH", "AL", emptyList(), emptyList()).range)
    }

    @Test
    fun `두 끝이 같으면 등급 하나만 쓴다`() {
        assertEquals("IM2", SessionGrade(emptyList(), "IM2", "IM2", emptyList(), emptyList()).range)
    }
}

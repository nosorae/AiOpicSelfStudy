package io.github.nosorae.aiopic.engine.answer.check

import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelChoiceTest {
    private val stable = ModelInfo(provider = "p", id = "stable-1", displayName = "Stable", hearsAudio = true, stability = Stability.Stable)
    private val preview = ModelInfo(provider = "p", id = "preview-1", displayName = "Preview", hearsAudio = true, stability = Stability.Preview)
    private val catalog = object : ModelCatalog {
        override val models = listOf(stable, preview)
        override val defaultModel = stable
    }

    @Test
    fun `F5_7 고르지 않았으면 기본 모델을 알림 없이 쓴다`() {
        assertEquals(ModelChoice(stable, fellBackFrom = null), catalog.choose(selectedId = null))
    }

    @Test
    fun `F5_8 고른 모델이 목록에 있으면 그 모델을 쓴다`() {
        assertEquals(ModelChoice(preview, fellBackFrom = null), catalog.choose(selectedId = "preview-1"))
    }

    @Test
    fun `F5_8 고른 모델이 앱 목록에서 빠졌으면 기본 모델로 돌리고 알린다`() {
        assertEquals(ModelChoice(stable, fellBackFrom = "gone-1"), catalog.choose(selectedId = "gone-1"))
    }

    @Test
    fun `F5_8 고른 모델이 없어졌다는 응답을 받으면 기본 모델로 돌리고 알린다`() {
        assertEquals(ModelChoice(stable, fellBackFrom = "preview-1"), catalog.choose(selectedId = "preview-1", unavailable = true))
    }
}

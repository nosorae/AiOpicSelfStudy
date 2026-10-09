package io.github.nosorae.aiopic.ui.featuresettings

import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.ui.featuresettings.model.ModelSelectionStore
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeModeStore
import kotlinx.coroutines.flow.MutableStateFlow

/** 안정 모델 둘과 미리보기 모델 하나. 실제 Gemini 목록에는 아직 미리보기 모델이 없어 안내를 확인하려고 만든다. */
object FakeCatalog : ModelCatalog {
    val stable = ModelInfo("gemini", "stable-a", "Stable A", hearsAudio = true, stability = Stability.Stable)
    val otherStable = ModelInfo("gemini", "stable-b", "Stable B", hearsAudio = true, stability = Stability.Stable)
    val preview = ModelInfo("gemini", "preview-c", "Preview C", hearsAudio = true, stability = Stability.Preview)

    override val defaultModel = stable
    override val models = listOf(stable, otherStable, preview)
}

class FakeSettingsStore(var modelId: String? = null) :
    ModelSelectionStore,
    ThemeModeStore {
    override val themeMode = MutableStateFlow(ThemeMode.System)

    override suspend fun selectedModelId(): String? = modelId

    override suspend fun selectModel(id: String) {
        modelId = id
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}

class FakeKeyVault(vararg saved: Pair<String, String>) : KeyVault {
    val keys = mutableMapOf(*saved)

    override suspend fun get(provider: String): String? = keys[provider]

    override suspend fun put(provider: String, key: String) {
        keys[provider] = key
    }

    override suspend fun delete(provider: String) {
        keys.remove(provider)
    }
}

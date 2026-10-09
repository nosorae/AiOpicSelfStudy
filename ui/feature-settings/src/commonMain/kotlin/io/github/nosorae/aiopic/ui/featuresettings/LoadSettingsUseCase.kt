package io.github.nosorae.aiopic.ui.featuresettings

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.check.choose
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider
import io.github.nosorae.aiopic.ui.featuresettings.key.KeyStatus
import io.github.nosorae.aiopic.ui.featuresettings.model.ModelSelectionStore
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeModeStore

data class LoadedSettings(
    val models: List<ModelInfo>,
    val selectedModelId: String,
    val fellBackFrom: String?,
    val keys: List<KeyStatus>,
    val themeMode: ThemeMode,
)

/**
 * 설정 화면에 보일 값을 읽는다. 고른 모델이 앱 업데이트로 목록에서 빠졌으면 기본 모델을 저장해 두고 [LoadedSettings.fellBackFrom] 으로
 * 알린다(F5.8). 분석 중에 모델이 내려가 앱이 기본 모델로 돌린 경우도 같이 알린다. 기본 모델을 저장하므로 알림은 한 번만 나온다.
 */
// ponytail: 첫 버전은 제공사가 Gemini 하나라 모델 목록도 하나만 받는다. 제공사를 더하면 제공사마다 목록을 받는다.
@Inject
class LoadSettingsUseCase(
    private val modelCatalog: ModelCatalog,
    private val modelSelectionStore: ModelSelectionStore,
    private val themeModeStore: ThemeModeStore,
    private val keyVault: KeyVault,
) {
    suspend operator fun invoke(): LoadedSettings {
        val choice = modelCatalog.choose(modelSelectionStore.selectedModelId())
        if (choice.fellBackFrom != null) modelSelectionStore.selectModel(choice.model.id)
        return LoadedSettings(
            models = modelCatalog.models,
            selectedModelId = choice.model.id,
            fellBackFrom = choice.fellBackFrom ?: modelSelectionStore.takeFellBackFrom(),
            keys = KeyProvider.entries.map { KeyStatus(it, saved = keyVault.get(it.id) != null) },
            themeMode = themeModeStore.themeMode.value,
        )
    }
}

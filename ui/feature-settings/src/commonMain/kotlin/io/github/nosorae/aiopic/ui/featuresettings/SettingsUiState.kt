package io.github.nosorae.aiopic.ui.featuresettings

import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider
import io.github.nosorae.aiopic.ui.featuresettings.key.KeyStatus
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode

data class SettingsUiState(
    val models: List<ModelInfo> = emptyList(),
    val selectedModelId: String? = null,
    /** 고른 모델이 목록에서 빠져 기본 모델로 돌렸을 때 빠진 모델 id. 모델을 다시 고르면 지운다. */
    val fellBackFrom: String? = null,
    val keys: List<KeyStatus> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.System,
    /** 지우기 확인을 묻는 중인 제공사. */
    val deletingKey: KeyProvider? = null,
) {
    val selectedModel: ModelInfo? get() = models.firstOrNull { it.id == selectedModelId }

    /** 미리보기 모델은 2주 공지로 내려갈 수 있어서 골라 둔 동안 안내한다(F5.8). */
    val showsPreviewNotice: Boolean get() = selectedModel?.stability == Stability.Preview
}

sealed interface SettingsIntent {
    /** 키 입력 화면에서 돌아왔을 때 키 저장 여부를 다시 읽는다. */
    data object Reload : SettingsIntent

    data class SelectModel(val id: String) : SettingsIntent

    data class SelectThemeMode(val mode: ThemeMode) : SettingsIntent

    data class RequestKeyDeletion(val provider: KeyProvider) : SettingsIntent

    data object DeleteKey : SettingsIntent

    data object CancelKeyDeletion : SettingsIntent
}

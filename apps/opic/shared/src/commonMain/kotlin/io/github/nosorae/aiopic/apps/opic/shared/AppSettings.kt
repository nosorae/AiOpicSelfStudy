package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.ui.featuresettings.model.ModelSelectionStore
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeModeStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 설정 값(화면 모드, 고른 분석 모델)을 플랫폼 설정 저장소에 둔다. 플랫폼은 문자열 읽기와 쓰기 두 함수만 넘긴다
 * (Android SharedPreferences, iOS NSUserDefaults). 키가 아니라서 암호화하지 않는다.
 */
class AppSettings(private val read: (String) -> String?, private val write: (String, String) -> Unit) :
    ThemeModeStore,
    ModelSelectionStore {
    private val mode = MutableStateFlow(ThemeMode.entries.firstOrNull { it.name == read(THEME_MODE) } ?: ThemeMode.System)
    override val themeMode: StateFlow<ThemeMode> = mode.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        write(THEME_MODE, mode.name)
        this.mode.value = mode
    }

    override suspend fun selectedModelId(): String? = read(MODEL_ID)

    override suspend fun selectModel(id: String) = write(MODEL_ID, id)

    companion object {
        private const val THEME_MODE = "theme_mode"
        private const val MODEL_ID = "model_id"

        /** 앱 틀 테스트처럼 기기에 남길 필요가 없을 때 쓴다. */
        fun inMemory(): AppSettings {
            val values = mutableMapOf<String, String>()
            return AppSettings(read = { values[it] }, write = { key, value -> values[key] = value })
        }
    }
}

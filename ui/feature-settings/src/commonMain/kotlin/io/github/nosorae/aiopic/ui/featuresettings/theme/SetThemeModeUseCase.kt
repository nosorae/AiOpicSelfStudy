package io.github.nosorae.aiopic.ui.featuresettings.theme

import dev.zacsweers.metro.Inject

@Inject
class SetThemeModeUseCase(private val themeModeStore: ThemeModeStore) {
    suspend operator fun invoke(mode: ThemeMode) = themeModeStore.setThemeMode(mode)
}

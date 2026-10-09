package io.github.nosorae.aiopic.ui.featuresettings.theme

import kotlinx.coroutines.flow.StateFlow

/**
 * 고른 화면 모드를 기기에 남긴다(F9.1). 앱 틀이 [themeMode] 를 구독해서 고르는 즉시 앱 전체 테마가 바뀐다.
 * 실제 저장은 앱 층이 플랫폼 설정 저장소로 넣고, 테스트는 메모리 가짜를 쓴다.
 */
interface ThemeModeStore {
    val themeMode: StateFlow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}

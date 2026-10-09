package io.github.nosorae.aiopic.ui.featuresettings.theme

/**
 * 화면 모드(F9.1). 기본은 [System] 이다. Android 9 에는 시스템 다크 설정이 없어 [Dark] 를 골라야 다크를 쓸 수 있다.
 * 앱 틀이 이 값으로 Montage 라이트, 다크 토큰을 고른다.
 */
enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }
}

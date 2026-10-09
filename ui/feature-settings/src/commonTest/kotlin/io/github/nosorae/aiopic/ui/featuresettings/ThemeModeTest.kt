package io.github.nosorae.aiopic.ui.featuresettings

import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeTest {
    @Test
    fun `F9_1 시스템 따라가기만 시스템 설정을 따르고 라이트와 다크는 시스템 설정과 상관없다`() {
        val table = ThemeMode.entries.associateWith { mode -> listOf(false, true).map { mode.isDark(systemDark = it) } }

        assertEquals(
            mapOf(
                ThemeMode.System to listOf(false, true),
                ThemeMode.Light to listOf(false, false),
                ThemeMode.Dark to listOf(true, true),
            ),
            table,
        )
    }
}

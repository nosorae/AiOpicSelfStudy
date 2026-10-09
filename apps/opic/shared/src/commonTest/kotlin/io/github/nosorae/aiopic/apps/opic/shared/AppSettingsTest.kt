package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppSettingsTest {
    private val disk = mutableMapOf<String, String>()

    private fun reopen() = AppSettings(read = { disk[it] }, write = { key, value -> disk[key] = value })

    @Test
    fun `F9_1 처음 켜면 시스템 따라가기이고 모델은 고르지 않은 상태다`() = runTest {
        val settings = reopen()

        assertEquals(ThemeMode.System, settings.themeMode.value)
        assertNull(settings.selectedModelId())
    }

    @Test
    fun `F9_1 F5_8 고른 화면 모드와 모델은 앱을 다시 켜도 남는다`() = runTest {
        reopen().run {
            setThemeMode(ThemeMode.Dark)
            selectModel("gemini-3.5-flash-lite")
        }

        val reopened = reopen()

        assertEquals(ThemeMode.Dark, reopened.themeMode.value)
        assertEquals("gemini-3.5-flash-lite", reopened.selectedModelId())
    }

    @Test
    fun `F9_1 화면 모드를 고르면 구독하는 쪽이 바로 새 값을 본다`() = runTest {
        val settings = reopen()

        settings.setThemeMode(ThemeMode.Light)

        assertEquals(ThemeMode.Light, settings.themeMode.value)
    }

    @Test
    fun `저장된 화면 모드를 읽을 수 없으면 시스템 따라가기로 시작한다`() {
        disk["theme_mode"] = "Sepia"

        assertEquals(ThemeMode.System, reopen().themeMode.value)
    }
}

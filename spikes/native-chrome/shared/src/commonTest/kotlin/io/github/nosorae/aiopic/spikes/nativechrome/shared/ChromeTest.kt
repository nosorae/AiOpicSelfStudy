package io.github.nosorae.aiopic.spikes.nativechrome.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class ChromeTest {
    @Test
    fun tabBarVisibilityDiffersOnlyForSubScreens() {
        val table = Screen.entries.associateWith { it.showsTabBar(Platform.Android) to it.showsTabBar(Platform.Ios) }
        assertEquals(true to true, table[Screen.Home])
        assertEquals(false to true, table[Screen.ReportDetail])
        assertEquals(false to false, table[Screen.Exam])
    }
}

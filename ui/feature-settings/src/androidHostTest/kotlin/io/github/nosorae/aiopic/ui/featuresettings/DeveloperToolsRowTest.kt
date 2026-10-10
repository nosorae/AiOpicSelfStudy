package io.github.nosorae.aiopic.ui.featuresettings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** 앱이 여는 함수를 넘길 때(디버그 빌드)만 설정 끝에 "개발자 도구" 줄이 보인다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class DeveloperToolsRowTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun `여는 함수를 넘기면 줄이 보이고 누르면 연다`() {
        var opened = 0
        rule.setContent { MontageTheme { screen(onOpenDeveloperTools = { opened++ }) } }

        rule.onNodeWithTag("settings-developer-tools").performScrollTo().performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `여는 함수가 없으면 줄이 없다`() {
        rule.setContent { MontageTheme { screen(onOpenDeveloperTools = null) } }

        rule.onAllNodesWithTag("settings-developer-tools").assertCountEquals(0)
    }

    @androidx.compose.runtime.Composable
    private fun screen(onOpenDeveloperTools: (() -> Unit)?) = SettingsScreen(
        SettingsSamples.default,
        onIntent = {},
        onOpenKeyEntry = {},
        onOpenFreeTierNotice = {},
        onOpenFlagExport = {},
        onOpenAppInfo = {},
        onPickQuestionFile = {},
        onOpenDeveloperTools = onOpenDeveloperTools,
    )
}

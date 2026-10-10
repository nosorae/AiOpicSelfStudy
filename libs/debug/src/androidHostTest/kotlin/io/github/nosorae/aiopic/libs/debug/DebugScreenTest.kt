package io.github.nosorae.aiopic.libs.debug

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DebugScreenTest {
    @get:Rule val rule = createComposeRule()

    private val platform = MemoryPlatform(info = listOf("app" to "1.0 (7)"))
    private val core = DebugCore(Clock.System).apply { install(DebugConfig(), platform) }
    private var shared: String? = null

    private fun show() = rule.setContent { DebugScreen(core, onShare = { shared = it }, onClose = {}) }

    @Test
    fun searchAndLevelFilterNarrowTheLogs() {
        core.log(DebugLevel.Info, "Exam", "문항 3 시작")
        core.log(DebugLevel.Warn, "Key", "키 확인 실패 401")
        show()

        rule.onNodeWithText("문항 3 시작", substring = true).assertExists()
        rule.onNodeWithText("Warn").performClick()
        rule.onAllNodesWithText("문항 3 시작", substring = true).assertCountEquals(0)
        rule.onNodeWithText("키 확인 실패 401", substring = true).assertExists()

        rule.onNodeWithText("All").performClick()
        rule.onNodeWithText("Search").performTextInput("문항")
        rule.onAllNodesWithText("키 확인 실패 401", substring = true).assertCountEquals(0)
    }

    @Test
    fun tappingACallShowsItsBodies() {
        core.recordCall(
            DebugCall(
                time = Instant.parse("2026-10-10T01:00:00Z"),
                method = "POST",
                url = "https://x.dev/v1/models",
                status = 400,
                duration = 80.milliseconds,
                requestHeaders = listOf("x-goog-api-key" to "***"),
                requestBody = "{}",
                responseHeaders = emptyList(),
                responseBody = "bad key body",
            ),
        )
        show()

        rule.onNodeWithText("Network").performClick()
        rule.onAllNodesWithText("bad key body", substring = true).assertCountEquals(0)
        rule.onNodeWithText("https://x.dev/v1/models", substring = true).performClick()

        rule.onNodeWithText("bad key body", substring = true).assertExists()
        rule.onNodeWithText("Copy cURL").assertExists()
    }

    @Test
    fun togglingASwitchStoresIt() {
        core.switch("slow", "Slow response")
        show()

        rule.onNodeWithText("Switches").performClick()
        rule.onNodeWithText("Slow response").performClick()

        assertEquals(true, platform.readSwitch("slow"))
    }

    @Test
    fun panelsInfoAndCrashesAreShown() {
        core.panel("Queue") { listOf("pending" to "2") }
        platform.writeCrash("crash-1.txt", "IllegalStateException: boom")
        show()

        rule.onNodeWithText("Panels").performClick()
        rule.onNodeWithText("pending: 2").assertExists()
        rule.onNodeWithText("Info").performClick()
        rule.onNodeWithText("app: 1.0 (7)").assertExists()
        rule.onNodeWithText("Crashes").performClick()
        rule.onNodeWithText("IllegalStateException: boom", substring = true).assertExists()
        rule.onNodeWithText("Delete").performClick()

        assertEquals(emptyList(), platform.crashNames())
    }

    @Test
    fun shareSendsTheWholeReport() {
        core.log(DebugLevel.Error, "Rec", "녹음 실패")
        show()

        rule.onNodeWithText("Share").performClick()
        rule.waitUntil { shared != null }

        assertContains(shared.orEmpty(), "E Rec: 녹음 실패")
    }
}

package io.github.nosorae.aiopic.libs.debug

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
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

    /** 이름과 값이 한 줄로 묶여 읽히는지 본다. */
    private fun row(name: String, value: String) = hasText(name) and hasText(value)

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
        rule.onNodeWithText("Request").performClick()
        rule.onNodeWithText("{}").assertExists()
        rule.onNodeWithText("Headers").performClick()
        rule.onNodeWithText("x-goog-api-key", substring = true).assertExists()

        rule.onNodeWithText("Copy cURL").performClick()
        val clipboard = ApplicationProvider.getApplicationContext<Context>().getSystemService(ClipboardManager::class.java)
        assertEquals("curl -X POST 'https://x.dev/v1/models' -H 'x-goog-api-key: ***' --data-raw '{}'", clipboard.primaryClip?.getItemAt(0)?.text.toString())
        rule.onNodeWithText("Copied").assertExists()
    }

    @Test
    fun responseJsonIsIndented() {
        core.recordCall(
            DebugCall(
                time = Instant.parse("2026-10-10T01:00:00Z"),
                method = "GET",
                url = "https://x.dev/v1/a",
                requestHeaders = emptyList(),
                requestBody = "",
                status = 200,
                duration = 80.milliseconds,
                responseBody = "{\"ok\":true}",
            ),
        )
        show()

        rule.onNodeWithText("Network").performClick()
        rule.onNodeWithText("/v1/a", substring = true).performClick()

        rule.onNodeWithText("{\n  \"ok\": true\n}").assertExists()
    }

    @Test
    fun logsFollowTheNewestLineUntilFollowIsOff() {
        (1..60).forEach { core.log(DebugLevel.Info, "T", "line $it") }
        show()

        rule.onNodeWithText("line 60", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Follow").performClick()
        (61..70).forEach { core.log(DebugLevel.Info, "T", "line $it") }
        rule.waitForIdle()
        // 미리 그려 둔 줄이 있을 수 있어 노드가 있으면 화면에 안 보이는지만 본다.
        rule.onAllNodesWithText("line 70", substring = true).let { nodes ->
            if (nodes.fetchSemanticsNodes().isNotEmpty()) nodes.onFirst().assertIsNotDisplayed()
        }

        rule.onNodeWithText("Follow").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("line 70", substring = true).assertIsDisplayed()
    }

    @Test
    fun emptyTabsSayWhatWillAppear() {
        show()

        rule.onNodeWithText("No logs yet", substring = true).assertExists()
        rule.onNodeWithText("Network").performClick()
        rule.onNodeWithText("No requests yet", substring = true).assertExists()
        rule.onNodeWithText("Switches").performClick()
        rule.onNodeWithText("No switches yet", substring = true).assertExists()
        rule.onNodeWithText("Panels").performClick()
        rule.onNodeWithText("No panels yet", substring = true).assertExists()
        rule.onNodeWithText("Crashes").performClick()
        rule.onNodeWithText("No crashes recorded", substring = true).assertExists()
    }

    @Test
    fun switchesOffTheirDefaultAreMarked() {
        core.switch("slow", "Slow response")
        show()

        rule.onNodeWithText("Switches").performClick()
        rule.onAllNodesWithText("Changed", substring = true).assertCountEquals(0)
        rule.onNodeWithText("Slow response").performClick()

        rule.onNodeWithText("1 switch differs from its default", substring = true).assertExists()
    }

    @Test
    fun togglingASwitchStoresIt() {
        core.switch("slow", "Slow response")
        show()

        rule.onNodeWithText("Switches").performClick()
        rule.onNodeWithText("Slow response").performClick()

        assertEquals(true, platform.readSwitch("slow"))
        rule.onNode(hasText("Slow response") and isToggleable()).assertIsOn()
    }

    @Test
    fun panelsInfoAndCrashesAreShown() {
        core.panel("Queue") { listOf("pending" to "2") }
        platform.writeCrash("crash-1.txt", "IllegalStateException: boom")
        show()

        rule.onNodeWithText("Panels").performClick()
        rule.onNode(row("pending", "2")).assertExists()
        rule.onNodeWithText("Info").performClick()
        rule.onNode(row("app", "1.0 (7)")).assertExists()
        rule.onNodeWithText("Crashes").performClick()
        rule.onNodeWithText("IllegalStateException: boom", substring = true).assertExists()
        rule.onNodeWithText("Delete").performClick()

        assertEquals(emptyList(), platform.crashNames())
    }

    @Test
    fun panelValuesAreRedactedOnScreen() {
        val key = "AIza" + "x".repeat(35)
        core.panel("Key") { listOf("key" to key) }
        core.panel("Broken") { error("bad $key") }
        show()

        rule.onNodeWithText("Panels").performClick()
        rule.onNode(row("key", "***")).assertExists()
        rule.onNode(row("error", "bad ***")).assertExists()
        rule.onAllNodesWithText(key, substring = true).assertCountEquals(0)
    }

    @Test
    fun shareSendsTheWholeReport() {
        core.log(DebugLevel.Error, "Rec", "녹음 실패")
        show()

        rule.onNodeWithText("Share").performClick()
        rule.waitUntil { shared != null }

        assertContains(shared.orEmpty(), "E Rec: 녹음 실패")
    }

    @Test
    fun sharingKeepsTheButtonAndBlocksOtherInputUntilTheReportIsReady() {
        val gate = CompletableDeferred<Unit>()
        core.panel("Slow") {
            gate.await()
            emptyList()
        }
        show()

        rule.onNodeWithText("Share").performClick()
        rule.onNodeWithText("Share").assertIsNotEnabled()
        rule.onNodeWithText("Close").assertIsNotEnabled()
        rule.onNodeWithText("Network").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Preparing report").assertExists()

        gate.complete(Unit)
        rule.waitUntil { shared != null }
        rule.onNodeWithText("Share").assertIsEnabled()
        rule.onNodeWithText("Close").assertIsEnabled()
    }

    @Test
    fun reduceMotionDrawsTheWaitingBarAsOneStillFrame() {
        val gate = CompletableDeferred<Unit>()
        core.panel("Slow") {
            gate.await()
            emptyList()
        }
        rule.setContent { DebugScreen(core, onShare = {}, onClose = {}, reduceMotion = true) }

        rule.onNodeWithText("Panels").performClick()

        rule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.4f, 0f..1f))).assertExists()
        rule.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertCountEquals(0)
        gate.complete(Unit)
    }

    @Test
    fun aLogRowWithAnExceptionIsTallEnoughToTap() {
        core.log(DebugLevel.Error, "Rec", "recording failed", IllegalStateException("timeout"))
        show()

        rule.onNode(hasClickAction() and hasText("recording failed", substring = true)).assertHeightIsAtLeast(48.dp)
    }
}

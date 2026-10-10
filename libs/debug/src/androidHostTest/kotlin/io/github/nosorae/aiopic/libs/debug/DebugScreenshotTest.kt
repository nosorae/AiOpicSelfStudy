package io.github.nosorae.aiopic.libs.debug

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** 탭마다 같은 기록으로 라이트, 다크, 글자 200% 를 찍는다. 시각을 고정해 찍을 때마다 같은 그림이 나온다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class DebugScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val at = Instant.parse("2026-10-10T01:00:00Z")
    private val platform = MemoryPlatform(info = listOf("app" to "io.github.nosorae.aiopic 1.0 (7)", "os" to "Android 15", "free storage" to "41200 MB"))
    private val core = DebugCore(
        object : Clock {
            override fun now() = at
        },
    ).apply { install(DebugConfig(), platform) }

    private fun records() = core.apply {
        log(DebugLevel.Debug, "Exam", "question 3 loaded")
        log(DebugLevel.Info, "Rec", "recording started")
        log(DebugLevel.Warn, "Key", "key check failed: 400")
        log(DebugLevel.Error, "Analysis", "analysis failed", IllegalStateException("timeout"))
        call("GET", "https://generativelanguage.googleapis.com/v1beta/models?key=***", 200, 84, "{\"models\":[{\"name\":\"models/gemini\"}]}")
        call("POST", "https://generativelanguage.googleapis.com/v1beta/models/gemini:generateContent", 200, 2_140, "{\"candidates\":[]}")
        recordCall(
            DebugCall(
                time = at,
                method = "POST",
                url = "https://generativelanguage.googleapis.com/v1beta/models/gemini:generateContent",
                status = 400,
                duration = 312.milliseconds,
                requestHeaders = listOf("x-goog-api-key" to "***", "Content-Type" to "application/json"),
                requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"Describe your home\"}]}]}",
                responseHeaders = listOf("Content-Type" to "application/json"),
                responseBody = "{\"error\":{\"code\":400,\"message\":\"API key not valid\",\"status\":\"INVALID_ARGUMENT\",\"retry\":false}}",
            ),
        )
        call("GET", "https://example.com/health", null, 5_000, "", error = "SocketTimeoutException: 5000 ms")
    }

    private fun DebugCore.call(method: String, url: String, status: Int?, ms: Long, body: String, error: String? = null) = recordCall(
        DebugCall(at, method, url, emptyList(), "", status, ms.milliseconds, responseBody = body, error = error),
    )

    private fun shot(dark: Boolean = false, records: Boolean = true, act: () -> Unit = {}) {
        if (records) records()
        rule.setContent { DebugApp(core, onShare = {}, onClose = {}, dark = dark) }
        act()
        rule.onRoot().captureRoboImage()
    }

    private fun openCall() {
        rule.onNodeWithText("Network").performClick()
        rule.onNodeWithText("POST 400", substring = true).performClick()
    }

    @Test fun logs() = shot()

    @Test fun logsDark() = shot(dark = true)

    @Config(fontScale = 2f)
    @Test
    fun logsFontScale200() = shot()

    @Test fun logsSearch() = shot { rule.onNodeWithText("Search").performTextInput("key") }

    @Test fun network() = shot { rule.onNodeWithText("Network").performClick() }

    @Test fun networkDark() = shot(dark = true) { rule.onNodeWithText("Network").performClick() }

    @Config(fontScale = 2f)
    @Test
    fun networkFontScale200() = shot { rule.onNodeWithText("Network").performClick() }

    @Test fun networkCallOpened() = shot { openCall() }

    @Test fun networkCallOpenedDark() = shot(dark = true) { openCall() }

    @Test
    fun networkCallHeaders() = shot {
        openCall()
        rule.onNodeWithText("Headers").performClick()
    }

    @Test
    fun switchesChanged() = shot {
        core.switch("fake_gemini", "Fake Gemini responses")
        core.switch("slow_response", "Slow response (3 s)")
        core.setSwitch("fake_gemini", true)
        rule.onNodeWithText("Switches").performClick()
    }

    @Test
    fun panels() = shot {
        core.panel("Work queue") { listOf("pending" to "2", "last error" to "HTTP 429 at 01:00:00") }
        core.panel("Audio") { listOf("route" to "speaker", "key" to "***") }
        rule.onNodeWithText("Panels").performScrollTo().performClick()
    }

    @Test
    fun crashes() = shot {
        platform.writeCrash(
            "crash-1791594000000.txt",
            "# Crash 2026-10-10T01:00:00Z\njava.lang.IllegalStateException: recorder released\n" +
                (1..12).joinToString("\n") { "\tat io.github.nosorae.aiopic.Recorder.stop(Recorder.kt:$it)" },
        )
        rule.onNodeWithText("Crashes").performScrollTo().performClick()
    }

    @Test fun empty() = shot(records = false)

    @Test fun emptyDark() = shot(dark = true, records = false) { rule.onNodeWithText("Crashes").performScrollTo().performClick() }
}

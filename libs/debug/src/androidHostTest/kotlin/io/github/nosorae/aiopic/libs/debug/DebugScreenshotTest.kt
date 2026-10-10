package io.github.nosorae.aiopic.libs.debug

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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

/** 로그와 네트워크 탭을 같은 기록으로 찍는다. 시각을 고정해 찍을 때마다 같은 그림이 나온다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class DebugScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val core = DebugCore(
        object : Clock {
            override fun now() = Instant.parse("2026-10-10T01:00:00Z")
        },
    ).apply {
        install(DebugConfig(), MemoryPlatform())
        log(DebugLevel.Debug, "Exam", "question 3 loaded")
        log(DebugLevel.Info, "Rec", "recording started")
        log(DebugLevel.Warn, "Key", "key check failed: 400")
        log(DebugLevel.Error, "Analysis", "analysis failed", IllegalStateException("timeout"))
        recordCall(
            DebugCall(
                time = Instant.parse("2026-10-10T01:00:00Z"),
                method = "POST",
                url = "https://generativelanguage.googleapis.com/v1beta/models/gemini:generateContent",
                status = 400,
                duration = 312.milliseconds,
                requestHeaders = listOf("x-goog-api-key" to "***", "Content-Type" to "application/json"),
                requestBody = "{\"contents\":[]}",
                responseHeaders = listOf("Content-Type" to "application/json"),
                responseBody = "{\"error\":{\"code\":400,\"message\":\"API key not valid\"}}",
            ),
        )
    }

    @Test
    fun logs() {
        rule.setContent { DebugApp(core, onShare = {}, onClose = {}) }
        rule.onRoot().captureRoboImage()
    }

    @Test
    fun networkCallOpened() {
        rule.setContent { DebugApp(core, onShare = {}, onClose = {}) }
        rule.onNodeWithText("Network").performClick()
        rule.onNodeWithText("POST 400", substring = true).performClick()
        rule.onRoot().captureRoboImage()
    }
}

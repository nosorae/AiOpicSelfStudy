package io.github.nosorae.aiopic.ui.featurepractice

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 녹음 중에 위쪽 X 를 눌러 연 나가기 확인 대화상자. 대화상자는 따로 뜨는 창이라 화면 전체를 찍는다.
 * 목소리 띠가 계속 돌면 화면이 쉬지 않아 누르지 못하므로 동작 줄이기로 멈춘 장면에서 연다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class PracticeExitDialogScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun recording() = open()

    @Test fun recording_dark() = open(dark = true)

    @Config(fontScale = 2f)
    @Test
    fun recording_fontScale200() = open()

    private fun open(dark: Boolean = false) {
        rule.setContent { MontageTheme(darkTheme = dark, reduceMotion = true) { PracticeScreen(state = PracticeSamples.replayable, onIntent = {}) } }
        rule.onNodeWithContentDescription("나가기").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("연습을 나갈까요?").assertExists()
        captureScreenRoboImage()
    }
}

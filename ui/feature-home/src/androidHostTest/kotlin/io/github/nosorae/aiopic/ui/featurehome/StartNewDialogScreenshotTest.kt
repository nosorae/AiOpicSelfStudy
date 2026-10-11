package io.github.nosorae.aiopic.ui.featurehome

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 진행 중인 회차가 있을 때 새로 시작을 눌러 연 확인 대화상자(#223). 따로 뜨는 창이라 화면 전체를 라이트, 다크, 글자 200% 로 찍는다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class StartNewDialogScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun startNew() = open()

    @Test fun startNew_dark() = open(dark = true)

    @Config(fontScale = 2f)
    @Test
    fun startNew_fontScale200() = open()

    private fun open(dark: Boolean = false) {
        rule.setContent { MontageTheme(darkTheme = dark) { HomeScreen(state = HomeSamples.history, onExamClick = {}, onSessionClick = {}) } }
        rule.onNodeWithTag("exam-start").performClick()
        rule.waitForIdle()
        captureScreenRoboImage()
    }
}

package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 위쪽 X 를 눌러 연 나가기 확인 대화상자. 대화상자는 따로 뜨는 창이라 화면 전체를 찍는다. 본문마다 한 장, 문항 화면은 다크와 글자 200% 도 찍는다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ExitDialogScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun examTimer() = exam(ExamSamples.recalibrate)

    @Test fun examSurvey() = exam(ExamSamples.survey)

    @Test fun examLevel() = exam(ExamSamples.level)

    @Test fun examTimer_dark() = exam(ExamSamples.recalibrate, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun examTimer_fontScale200() = exam(ExamSamples.recalibrate)

    @Test fun settingsSurvey() = open { SurveyScreen(state = SurveySamples.edited, onIntent = {}, onClose = {}) }

    @Test fun settingsSurvey_dark() = open(dark = true) { SurveyScreen(state = SurveySamples.edited, onIntent = {}, onClose = {}) }

    private fun exam(state: ExamUiState, dark: Boolean = false) = open(dark) { ExamScreen(state = state, onIntent = {}) }

    private fun open(dark: Boolean = false, content: @Composable () -> Unit) {
        rule.setContent { MontageTheme(darkTheme = dark) { content() } }
        rule.onNodeWithContentDescription("나가기").performClick()
        rule.waitForIdle()
        captureScreenRoboImage()
    }
}

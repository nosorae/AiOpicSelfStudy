package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 질문을 기다리는 화면(F6.1). 기다림 표시가 계속 돌아서 시계가 흐르면 화면이 쉬지 않아 찍지 못한다.
 * 시계를 멈추고 1.2초 넘긴 장면을 찍는다. 시계를 멈추는 규칙이 다른 화면의 처리 중 표시 장면을 바꾸지 않게 클래스를 나눈다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ExamLoadingScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun questionSpeaking() = still(ExamSamples.questionSpeaking)

    @Test fun questionLoading() = still(ExamSamples.questionLoading)

    @Test fun questionLoadingRecalibrated() = still(ExamSamples.questionLoadingRecalibrated)

    @Test fun questionSlow() = still(ExamSamples.questionSlow)

    private fun still(state: ExamUiState) {
        rule.mainClock.autoAdvance = false
        rule.setContent { MontageTheme { ExamScreen(state = state, onIntent = {}) } }
        rule.mainClock.advanceTimeBy(1_200)
        rule.onRoot().captureRoboImage()
    }
}

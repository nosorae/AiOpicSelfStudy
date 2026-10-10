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
 * 녹음 중 화면(S6 장비 점검, S7 문항). 목소리 띠가 계속 움직이고 Next 채움이 10초 동안 차올라서 시계가 흐르면 화면이 쉬지 않는다.
 * 시계를 멈춘 첫 장면을 찍는다. 띠는 샘플의 음량만큼 부푼 모양, 채움은 샘플의 녹음 시간만큼 찬 모양이다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ExamRecordingScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun checkRecording() = still(ExamSamples.checkRecording)

    @Test fun questionReplayable() = still(ExamSamples.questionReplayable)

    @Test fun questionNextable() = still(ExamSamples.questionNextable)

    @Test fun questionSaving() = still(ExamSamples.questionSaving)

    @Test fun questionBuiltIn() = still(ExamSamples.questionBuiltIn)

    /** 설정에서 답변 타이머를 켰을 때(F2.7). 꺼져 있으면 "녹음 중"만 보인다. */
    @Test fun questionTimer() = still(ExamSamples.questionTimer)

    @Test fun questionReplayable_dark() = still(ExamSamples.questionReplayable, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun questionReplayable_fontScale200() = still(ExamSamples.questionReplayable)

    private fun still(state: ExamUiState, dark: Boolean = false) {
        rule.mainClock.autoAdvance = false
        rule.setContent { MontageTheme(darkTheme = dark) { ExamScreen(state = state, onIntent = {}) } }
        rule.mainClock.advanceTimeByFrame()
        rule.onRoot().captureRoboImage()
    }
}

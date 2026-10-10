package io.github.nosorae.aiopic.ui.featurepractice

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

/** 녹음 중 화면. 목소리 띠가 계속 움직여 시계를 멈춘 첫 장면을 찍는다. 라이트, 다크, 글자 200% 다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class PracticeRecordingScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun replayable() = still(PracticeSamples.replayable)

    @Test fun nextable() = still(PracticeSamples.nextable)

    @Test fun saving() = still(PracticeSamples.saving)

    @Test fun replayable_dark() = still(PracticeSamples.replayable, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun replayable_fontScale200() = still(PracticeSamples.replayable)

    private fun still(state: PracticeUiState, dark: Boolean = false) {
        rule.mainClock.autoAdvance = false
        rule.setContent { MontageTheme(darkTheme = dark) { PracticeScreen(state = state, onIntent = {}) } }
        rule.mainClock.advanceTimeByFrame()
        rule.onRoot().captureRoboImage()
    }
}

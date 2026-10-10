package io.github.nosorae.aiopic.ui.featurepractice

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 진행마다 라이트 한 장. 녹음 중 화면은 다크와 글자 200% 도 찍는다. 입력은 [PracticeSamples] 다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class PracticeScreenshotTest {
    @Test fun loading() = shot(PracticeSamples.loading)

    @Test fun ready() = shot(PracticeSamples.ready)

    @Test fun speaking() = shot(PracticeSamples.speaking)

    @Test fun replayable() = shot(PracticeSamples.replayable)

    @Test fun nextable() = shot(PracticeSamples.nextable)

    @Test fun saving() = shot(PracticeSamples.saving)

    @Test fun micError() = shot(PracticeSamples.micError)

    @Test fun noQuestion() = shot(PracticeSamples.noQuestion)

    @Test fun replayable_dark() = shot(PracticeSamples.replayable, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun replayable_fontScale200() = shot(PracticeSamples.replayable)

    private fun shot(state: PracticeUiState, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { PracticeScreen(state = state, onIntent = {}) }
    }
}

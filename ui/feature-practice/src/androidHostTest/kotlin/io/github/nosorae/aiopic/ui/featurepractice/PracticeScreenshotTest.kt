package io.github.nosorae.aiopic.ui.featurepractice

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 진행마다 라이트 한 장. 입력은 [PracticeSamples] 다. 질문을 고르고 읽는 화면과 녹음 중 화면은 물결과 목소리 띠가 계속 움직여 [PracticeRecordingScreenshotTest] 가 찍는다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class PracticeScreenshotTest {
    @Test fun ready() = shot(PracticeSamples.ready)

    @Test fun ready_dark() = shot(PracticeSamples.ready, dark = true)

    @Test fun readyAnyType() = shot(PracticeSamples.ready.copy(weakness = null))

    @Config(fontScale = 2f)
    @Test
    fun ready_fontScale200() = shot(PracticeSamples.ready)

    @Test fun micError() = shot(PracticeSamples.micError)

    @Test fun recordingError() = shot(PracticeSamples.recordingError)

    @Test fun noQuestion() = shot(PracticeSamples.noQuestion)

    private fun shot(state: PracticeUiState, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { PracticeScreen(state = state, onIntent = {}) }
    }
}

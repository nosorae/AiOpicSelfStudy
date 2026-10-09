package io.github.nosorae.aiopic.ui.featurehistory

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 기록이 찬 상태는 라이트, 다크, 글자 200% 를 찍고 빈 상태를 한 장 찍는다. 입력은 미리보기와 같은 [HistorySamples] 다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class HistoryScreenshotTest {
    @Test fun filled() = shot(HistorySamples.filled)

    @Test fun filled_dark() = shot(HistorySamples.filled, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun filled_fontScale200() = shot(HistorySamples.filled)

    @Test fun empty() = shot(HistorySamples.empty)

    private fun shot(state: HistoryUiState, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { HistoryScreen(state = state, onOpenSession = {}) }
    }
}

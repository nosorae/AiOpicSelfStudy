package io.github.nosorae.aiopic.ui.featurehome

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 상태마다 라이트 한 장, 지난 회차가 있는 상태는 다크와 글자 200% 도 찍는다. 입력은 미리보기와 같은 [HomeSamples] 다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class HomeScreenshotTest {
    @Test fun loading() = shot(HomeSamples.loading)

    @Test fun empty() = shot(HomeSamples.empty)

    @Test fun history() = shot(HomeSamples.history)

    @Test fun history_dark() = shot(HomeSamples.history, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun history_fontScale200() = shot(HomeSamples.history)

    private fun shot(state: HomeUiState, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { HomeScreen(state = state, onExamClick = {}, onPracticeClick = {}, onSessionClick = {}) }
    }
}

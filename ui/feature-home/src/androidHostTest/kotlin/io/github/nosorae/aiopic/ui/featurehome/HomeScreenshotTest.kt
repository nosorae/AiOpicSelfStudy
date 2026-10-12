package io.github.nosorae.aiopic.ui.featurehome

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 세 상태(마친 회차 없음, 진행 중 회차 있음, 마친 회차 있음)를 라이트, 다크, 글자 200% 로 찍는다(#230). 로딩은 라이트 한 장이다.
 * 입력은 미리보기와 같은 [HomeSamples] 다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class HomeScreenshotTest {
    @Test fun loading() = shot(HomeSamples.loading)

    @Test fun empty() = shot(HomeSamples.empty)

    @Test fun empty_dark() = shot(HomeSamples.empty, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun empty_fontScale200() = shot(HomeSamples.empty)

    @Test fun finished() = shot(HomeSamples.finished)

    @Test fun finished_dark() = shot(HomeSamples.finished, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun finished_fontScale200() = shot(HomeSamples.finished)

    @Test fun history() = shot(HomeSamples.history)

    @Test fun history_dark() = shot(HomeSamples.history, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun history_fontScale200() = shot(HomeSamples.history)

    private fun shot(state: HomeUiState, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { HomeScreen(state = state, onExamClick = {}, onSessionClick = {}) }
    }
}

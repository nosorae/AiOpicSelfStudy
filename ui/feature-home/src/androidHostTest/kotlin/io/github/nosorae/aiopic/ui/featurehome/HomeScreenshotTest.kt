package io.github.nosorae.aiopic.ui.featurehome

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 세 상태(마친 회차 없음, 진행 중 회차 있음, 마친 회차 있음)를 라이트, 다크, 글자 200% 로 찍는다(#230). 로딩은 라이트 한 장이다.
 * 글자 200% 와 동작 줄이기는 최근 회차 절까지 보이게 화면을 길게 잡는다.
 * 동작 줄이기는 진행 점과 등급 대기 물결이 있는 진행 중 상태로 찍고, 첫 프레임이 끝 장면과 같은지 함께 본다.
 * 입력은 미리보기와 같은 [HomeSamples] 다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class HomeScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun loading() = shot(HomeSamples.loading)

    @Test fun empty() = shot(HomeSamples.empty)

    @Test fun empty_dark() = shot(HomeSamples.empty, dark = true)

    @Config(fontScale = 2f, qualifiers = TALL)
    @Test
    fun empty_fontScale200() = shot(HomeSamples.empty)

    @Test fun finished() = shot(HomeSamples.finished)

    @Test fun finished_dark() = shot(HomeSamples.finished, dark = true)

    @Config(fontScale = 2f, qualifiers = TALL)
    @Test
    fun finished_fontScale200() = shot(HomeSamples.finished)

    @Test fun history() = shot(HomeSamples.history)

    @Test fun history_dark() = shot(HomeSamples.history, dark = true)

    @Config(fontScale = 2f, qualifiers = TALL)
    @Test
    fun history_fontScale200() = shot(HomeSamples.history)

    @Config(qualifiers = TALL)
    @Test
    fun history_reduceMotion() {
        show(HomeSamples.history, reduceMotion = true)
        val first = frame()
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        assertTrue("동작 줄이기인데 첫 프레임이 끝 장면과 다르다", first.sameAs(frame()))
        rule.onRoot().captureRoboImage()
    }

    // 등급을 기다리는 줄의 물결이 끝없이 돌아서 시계를 손으로 넘긴다.
    private fun show(state: HomeUiState, dark: Boolean = false, reduceMotion: Boolean = false) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MontageTheme(darkTheme = dark, reduceMotion = reduceMotion) { HomeScreen(state = state, onExamClick = {}, onSessionClick = {}) }
        }
    }

    private fun shot(state: HomeUiState, dark: Boolean = false) {
        show(state, dark)
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        rule.onRoot().captureRoboImage()
    }

    private fun frame(): Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()

    private companion object {
        const val SETTLE_MILLIS = 1_000L
        const val TALL = "w360dp-h1400dp-xhdpi"
    }
}

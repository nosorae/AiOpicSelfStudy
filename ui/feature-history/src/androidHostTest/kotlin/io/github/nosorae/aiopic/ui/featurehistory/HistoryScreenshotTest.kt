package io.github.nosorae.aiopic.ui.featurehistory

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 기록이 찬 상태는 라이트, 다크, 글자 200% 를 찍고 빈 상태를 한 장 찍는다. 입력은 미리보기와 같은 [HistorySamples] 다.
 * 테스트 시계를 멈추고 2.5초 넘겨 등급 구슬이 멈춘 끝 장면을 찍는다. 동작 줄이기면 첫 프레임이 끝 장면과 같은지도 본다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class HistoryScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun filled() = shot(HistorySamples.filled)

    @Test fun filled_dark() = shot(HistorySamples.filled, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun filled_fontScale200() = shot(HistorySamples.filled)

    @Test fun empty() = shot(HistorySamples.empty)

    @Test
    fun `최근 회차 등급 구슬이 눈금 위로 미끄러져 멈춘다`() {
        show(HistorySamples.filled)
        val first = frame()
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        assertFalse("움직이지 않는다", first.sameAs(frame()))
    }

    @Test
    fun `동작 줄이기면 등급 구슬과 약점 막대가 처음부터 끝 장면이다`() {
        show(HistorySamples.filled, reduceMotion = true)
        val first = frame()
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        assertTrue("동작 줄이기인데 움직인다", first.sameAs(frame()))
    }

    @Test
    fun `답변이 모두 분석되고 등급만 기다리는 회차는 등급 내는 중이다`() {
        val row = HistorySession(ExamSession(id = "s9", status = ExamStatus.Finished), emptyList(), answered = 12, analyzed = 12)
        show(HistoryUiState(loading = false, sessions = listOf(row)))
        rule.onNodeWithText("등급 내는 중").assertExists()
        rule.onNodeWithText("분석 12/12").assertDoesNotExist()
    }

    private fun show(state: HistoryUiState, dark: Boolean = false, reduceMotion: Boolean = false) {
        rule.mainClock.autoAdvance = false
        rule.setContent { MontageTheme(darkTheme = dark, reduceMotion = reduceMotion) { HistoryScreen(state = state, onOpenSession = {}) } }
    }

    private fun shot(state: HistoryUiState, dark: Boolean = false) {
        show(state, dark)
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        rule.onRoot().captureRoboImage()
    }

    private fun frame(): Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()

    private companion object {
        const val SETTLE_MILLIS = 2_500L
    }
}

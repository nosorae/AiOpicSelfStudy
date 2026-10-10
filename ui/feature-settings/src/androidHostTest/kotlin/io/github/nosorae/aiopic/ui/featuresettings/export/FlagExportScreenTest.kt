package io.github.nosorae.aiopic.ui.featuresettings.export

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 복사한 뒤의 체크는 동작 줄이기면 첫 프레임부터 다 그려져 있고 움직이지 않는다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class FlagExportScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun `동작 줄이기면 복사 완료 체크가 첫 프레임부터 다 그려져 있다`() {
        rule.mainClock.autoAdvance = false
        rule.setContent { MontageTheme(reduceMotion = true) { FlagExportScreen(state = FlagExportSamples.copied, onIntent = {}) } }
        val first = rule.onRoot().captureToImage().asAndroidBitmap()

        rule.mainClock.advanceTimeBy(2_000)

        assertTrue(first.sameAs(rule.onRoot().captureToImage().asAndroidBitmap()))
    }

    @Test
    fun `복사하면 체크가 그려지는 동안 프레임이 바뀐다`() {
        rule.mainClock.autoAdvance = false
        rule.setContent { MontageTheme { FlagExportScreen(state = FlagExportSamples.copied, onIntent = {}) } }
        val first = rule.onRoot().captureToImage().asAndroidBitmap()

        rule.mainClock.advanceTimeBy(2_000)

        assertTrue(!first.sameAs(rule.onRoot().captureToImage().asAndroidBitmap()))
    }
}

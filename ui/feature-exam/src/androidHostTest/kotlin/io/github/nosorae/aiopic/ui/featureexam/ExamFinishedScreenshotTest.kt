package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import io.github.nosorae.aiopic.libs.haptics.api.Haptics
import io.github.nosorae.aiopic.ui.design.LocalHaptics
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

/** 15문항을 다 마친 순간의 화면. 축하 조각이 터지는 중인 0.5초 장면과, 동작 줄이기면 멈춘 장면을 찍는다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ExamFinishedScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun finished() = shot()

    @Test fun finished_dark() = shot(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun finished_fontScale200() = shot()

    @Test fun finished_reduceMotion() = shot(reduceMotion = true)

    @Test
    fun `다 마친 화면은 축하 진동을 한 번 낸다`() {
        val played = mutableListOf<HapticEffect>()
        rule.setContent {
            MontageTheme { CompositionLocalProvider(LocalHaptics provides Haptics { played += it }) { ExamFinished(questionCount = 15) } }
        }

        rule.waitForIdle()

        assertEquals(listOf(HapticEffect.Celebrate), played)
    }

    private fun shot(dark: Boolean = false, reduceMotion: Boolean = false) {
        rule.mainClock.autoAdvance = false
        rule.setContent { MontageTheme(darkTheme = dark, reduceMotion = reduceMotion) { ExamFinished(questionCount = 15) } }
        rule.mainClock.advanceTimeBy(500)
        rule.onRoot().captureRoboImage()
    }
}

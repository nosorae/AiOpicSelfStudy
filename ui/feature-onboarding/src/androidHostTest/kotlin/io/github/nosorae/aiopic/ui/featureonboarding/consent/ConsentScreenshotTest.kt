package io.github.nosorae.aiopic.ui.featureonboarding.consent

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.featureonboarding.Res
import io.github.nosorae.aiopic.ui.featureonboarding.consent_free_tier
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 동의 화면은 상태가 하나라 라이트, 다크, 글자 200% 를 찍는다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ConsentScreenshotTest {
    @Test fun light() = shot()

    @Test fun dark() = shot(dark = true)

    @Config(fontScale = 2f)
    @Test
    fun fontScale200() = shot()

    @Test
    fun `F1_3 무료 등급 음성이 제품 개선에 쓰일 수 있다고 알린다`() {
        val text = runBlocking { getString(Res.string.consent_free_tier) }

        assertTrue(text, "무료" in text && "제품 개선" in text)
        assertTrue(Res.string.consent_free_tier in consentNotices)
    }

    private fun shot(dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { ConsentScreen(onIntent = {}) }
    }
}

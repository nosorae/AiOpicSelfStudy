package io.github.nosorae.aiopic.ui.featuresettings.info

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 앱 정보 화면의 첫 화면. 실제 고지 문서를 읽어 그린다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class AppInfoScreenshotTest {
    private val licenses = File("src/commonMain/composeResources/$LICENSES_FILE").readText()

    @Test fun light() = shot()

    @Test fun dark() = shot(dark = true)

    @Config(fontScale = 2f)
    @Test
    fun fontScale200() = shot()

    private fun shot(dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { AppInfoScreen(licenses) }
    }
}

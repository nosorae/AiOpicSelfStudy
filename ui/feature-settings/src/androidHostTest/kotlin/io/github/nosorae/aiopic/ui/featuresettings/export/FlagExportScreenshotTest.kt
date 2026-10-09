package io.github.nosorae.aiopic.ui.featuresettings.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 틀림 표시 내보내기의 상태마다 한 장. 복사한 뒤 화면은 다크와 글자 200% 도 찍는다. 입력은 미리보기와 같은 [FlagExportSamples] 다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class FlagExportScreenshotTest {
    @Test fun flagged() = capture(FlagExportSamples.flagged)

    @Test fun flagged_copied() = capture(FlagExportSamples.flagged, copied = true)

    @Test fun flagged_copied_dark() = capture(FlagExportSamples.flagged, copied = true, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun flagged_copied_fontScale200() = capture(FlagExportSamples.flagged, copied = true)

    @Test fun empty() = capture(FlagExportSamples.empty)

    private fun capture(state: FlagExportUiState, copied: Boolean = false, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { FlagExportScreen(state = state, copied = copied, onCopyClick = {}) }
    }
}

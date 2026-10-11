package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 컴포넌트마다 라이트, 다크 한 장씩. 그리는 내용은 `ComponentSamples.kt` 의 미리보기와 같다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ComponentsScreenshotTest {
    @Test fun button_light() = shot(dark = false) { ButtonSamples() }

    @Test fun button_dark() = shot(dark = true) { ButtonSamples() }

    @Test fun iconButton_light() = shot(dark = false) { IconButtonSamples() }

    @Test fun iconButton_dark() = shot(dark = true) { IconButtonSamples() }

    @Test fun chip_light() = shot(dark = false) { ChipSamples() }

    @Test fun chip_dark() = shot(dark = true) { ChipSamples() }

    @Test fun textField_light() = shot(dark = false) { TextFieldSamples() }

    @Test fun textField_dark() = shot(dark = true) { TextFieldSamples() }

    @Test fun progressBar_light() = shot(dark = false) { ProgressBarSamples() }

    @Test fun progressBar_dark() = shot(dark = true) { ProgressBarSamples() }

    @Test fun topBar_light() = shot(dark = false) { TopBarSamples() }

    @Test fun topBar_dark() = shot(dark = true) { TopBarSamples() }

    @Test fun alert_light() = shot(dark = false) { AlertSamples() }

    @Test fun alert_dark() = shot(dark = true) { AlertSamples() }

    @Config(fontScale = 2f)
    @Test
    fun alert_fontScale200() = shot(dark = false) { AlertSamples() }

    @Test fun listCell_light() = shot(dark = false) { ListCellSamples() }

    @Test fun listCell_dark() = shot(dark = true) { ListCellSamples() }

    @Test fun selectionControl_light() = shot(dark = false) { SelectionControlSamples() }

    @Test fun selectionControl_dark() = shot(dark = true) { SelectionControlSamples() }

    private fun shot(dark: Boolean, content: @Composable () -> Unit) = captureRoboImage {
        MontageTheme(darkTheme = dark) {
            Column(Modifier.background(MontageTheme.colors.backgroundNeutralPrimary)) { content() }
        }
    }
}

package io.github.nosorae.aiopic.ui.featuresettings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 설정 탭은 모든 묶음이 한 화면에 다 보이게 높이를 넉넉히 잡는다. 지우기, 철회 확인 대화상자는 창이 따로라 찍지 않는다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h1500dp-xhdpi")
class SettingsScreenshotTest {
    @Test fun light() = shot(SettingsSamples.default)

    @Test fun dark() = shot(SettingsSamples.dark, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun fontScale200() = shot(SettingsSamples.default)

    @Test fun previewSelected() = shot(SettingsSamples.previewSelected)

    @Test fun previewSelectedDark() = shot(SettingsSamples.previewSelected, dark = true)

    @Test fun fellBack() = shot(SettingsSamples.fellBack)

    /** 동의 철회 뒤, 답변 타이머 켬, 틀린 문항 파일을 고른 뒤(F1.3, F2.7, F6.3). */
    @Test fun practiceSet() = shot(SettingsSamples.practiceSet)

    @Test fun practiceSetDark() = shot(SettingsSamples.practiceSet, dark = true)

    @Test fun imported() = shot(SettingsSamples.imported)

    private fun shot(state: SettingsUiState, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { SettingsScreen(state, onIntent = {}, onOpenKeyEntry = {}, onOpenFreeTierNotice = {}, onOpenFlagExport = {}, onOpenAppInfo = {}, onPickQuestionFile = {}) }
    }
}

package io.github.nosorae.aiopic.ui.featureonboarding.key

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 상태마다 라이트 한 장, 오류 상태는 다크와 글자 200% 도 찍는다. 안내, 순서, 입력칸이 한 장에 다 보이게 높이를 넉넉히 잡는다. 입력은 미리보기와 같은 [KeyEntrySamples] 다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h1000dp-xhdpi")
class KeyEntryScreenshotTest {
    @Test fun empty() = shot(KeyEntrySamples.empty)

    @Test fun ready() = shot(KeyEntrySamples.ready)

    @Test fun checking() = shot(KeyEntrySamples.checking)

    @Test fun saved() = shot(KeyEntrySamples.saved)

    @Test fun invalid() = shot(KeyEntrySamples.invalid)

    @Test fun retryable() = shot(KeyEntrySamples.retryable)

    @Test fun invalid_dark() = shot(KeyEntrySamples.invalid, dark = true)

    @Config(fontScale = 2f, qualifiers = "w360dp-h1600dp-xhdpi")
    @Test
    fun invalid_fontScale200() = shot(KeyEntrySamples.invalid)

    private fun shot(state: KeyEntryUiState, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { KeyEntryScreen(state = state, onIntent = {}, onOpenFreeTierNotice = {}) }
    }
}

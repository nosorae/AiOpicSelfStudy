package io.github.nosorae.aiopic.spikes.nativechrome.shared

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 화면마다 한 벌. 탭 바는 앱 틀 것이라 여기 없다.
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ScreensScreenshotTest {
    @Test
    fun list() = captureRoboImage { ScreenContent(Screen.Home, {}) }

    @Test
    fun detail() = captureRoboImage { ScreenContent(Screen.ReportDetail, {}) }
}

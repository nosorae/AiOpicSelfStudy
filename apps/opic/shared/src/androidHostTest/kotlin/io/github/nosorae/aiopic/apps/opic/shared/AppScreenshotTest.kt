package io.github.nosorae.aiopic.apps.opic.shared

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 그래프에서 ViewModel 을 만들어 첫 화면까지 그려지는지 본다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class AppScreenshotTest {
    @Test
    fun app() {
        captureRoboImage { App(createAppGraph()) }
    }
}

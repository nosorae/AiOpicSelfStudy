package io.github.nosorae.aiopic.apps.opic.shared

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Android 앱 틀의 세 가지 대우(탭 화면, 하위 화면, 전체 화면)를 한 장씩 찍는다. 화면 안쪽 모양은 화면 모듈이 따로 찍는다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class AppShellScreenshotTest {
    @Test fun home() = shot(AppRoute.Home)

    @Config(qualifiers = "+night")
    @Test
    fun home_dark() = shot(AppRoute.Home)

    @Test fun reportDetail() = shot(AppRoute.Home, AppRoute.ReportDetail("s"))

    @Test fun exam() = shot(AppRoute.Home, AppRoute.Exam)

    /** 시스템은 라이트인데 설정에서 다크를 골랐다. 하단 탭까지 앱 전체가 다크로 그려져야 한다(F9.1). */
    @Test
    fun settings_appDarkOnLightSystem() = shot(AppRoute.Home, AppRoute.Settings, settings = darkSettings())

    private fun darkSettings() = AppSettings.inMemory().also { runBlocking { it.setThemeMode(ThemeMode.Dark) } }

    private fun shot(vararg stack: AppRoute, settings: AppSettings = AppSettings.inMemory()) = captureRoboImage {
        AppShell(testAppGraph(settings), startStack = stack.toList())
    }
}

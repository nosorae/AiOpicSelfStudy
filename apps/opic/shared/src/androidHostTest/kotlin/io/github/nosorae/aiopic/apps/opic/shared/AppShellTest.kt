package io.github.nosorae.aiopic.apps.opic.shared

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Android 앱 틀을 그래프째 띄워 탭 표시와 뒤로가기를 Activity 의 뒤로가기 디스패처로 확인한다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class AppShellTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun start(vararg stack: AppRoute) = rule.setContent { AppShell(createAppGraph(), startStack = stack.toList()) }

    private fun pressBack() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun `최상위 탭 화면에서는 하단 탭이 보인다`() {
        start(AppRoute.Home)

        rule.onNodeWithText("기록").assertIsDisplayed()
    }

    @Test
    fun `Android 하위 화면에서는 하단 탭을 숨긴다`() {
        start(AppRoute.Home, AppRoute.ReportDetail("s"))

        rule.onNodeWithText("기록").assertDoesNotExist()
    }

    @Test
    fun `하위 화면에서 뒤로가기를 누르면 홈으로 돌아가고 탭이 다시 보인다`() {
        start(AppRoute.Home, AppRoute.ReportDetail("s"))

        pressBack()

        rule.onNodeWithText("기록").assertIsDisplayed()
    }

    @Test
    fun `탭을 누르면 그 탭 화면으로 가고 뒤로가기로 홈에 돌아온다`() {
        start(AppRoute.Home)
        rule.onNodeWithText("설정").performClick()
        rule.onNodeWithText("모의시험 시작").assertDoesNotExist()

        pressBack()

        rule.onNodeWithText("모의시험 시작").assertIsDisplayed()
    }

    @Test
    fun `홈에서 모의시험 시작을 누르면 탭 없는 시험 화면이 열린다`() {
        start(AppRoute.Home)

        rule.onNodeWithText("모의시험 시작").performClick()

        rule.onNodeWithText("기록").assertDoesNotExist()
        rule.onNodeWithText("나가기").assertIsDisplayed()
    }

    @Test
    fun `시험 중 뒤로가기를 누르면 시험 화면에 머문다`() {
        start(AppRoute.Home, AppRoute.Exam)

        pressBack()

        rule.onNodeWithText("나가기").assertIsDisplayed()
    }

    @Test
    fun `시험 화면의 나가기를 누르면 홈으로 돌아간다`() {
        start(AppRoute.Home, AppRoute.Exam)

        rule.onNodeWithText("나가기").performClick()

        rule.onNodeWithText("모의시험 시작").assertIsDisplayed()
    }
}

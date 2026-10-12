package io.github.nosorae.aiopic.ui.featurehome

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 진행 중인 회차가 있을 때 새로 시작을 눌러 연 확인 대화상자(#223). 따로 뜨는 창이라 화면 전체를 라이트, 다크, 글자 200% 로 찍는다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class StartNewDialogScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun startNew() = open()

    @Test fun startNew_dark() = open(dark = true)

    @Config(fontScale = 2f)
    @Test
    fun startNew_fontScale200() = open()

    private fun open(dark: Boolean = false) {
        // 등급을 기다리는 줄의 물결은 끝없이 돌아 화면이 쉬지 않는다. 동작 줄이기로 멈춰 두어야 시계가 스스로 흘러 창이 다 그려진다.
        rule.setContent { MontageTheme(darkTheme = dark, reduceMotion = true) { HomeScreen(state = HomeSamples.history, onExamClick = {}, onSessionClick = {}) } }
        // 글자 200% 면 버튼이 화면 아래로 밀려 손가락 누름은 화면 밖을 누른다. 눌림 동작을 바로 부른다.
        rule.onNodeWithTag("exam-start").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithText("새 모의고사를 시작할까요?").assertExists()
        captureScreenRoboImage()
    }
}

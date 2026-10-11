package io.github.nosorae.aiopic.ui.design

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import io.github.nosorae.aiopic.libs.haptics.api.Haptics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 공통 확인 대화상자가 살짝 커지며 나타나고(동작 줄이기면 바로), 확인에만 경고 진동을 내고, 취소와 확인이 각자의 콜백만 부르는지 본다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class MontageConfirmDialogTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val played = mutableListOf<HapticEffect>()

    private fun show(reduceMotion: Boolean = false) = rule.setContent {
        CompositionLocalProvider(LocalHaptics provides Haptics { played += it }) {
            MontageTheme(reduceMotion = reduceMotion) {
                MontageConfirmDialog(
                    title = "모의고사를 나갈까요?",
                    message = "진행은 저장됩니다.",
                    confirm = "나가기",
                    cancel = "취소",
                    onConfirm = { calls += "confirm" },
                    onCancel = { calls += "cancel" },
                )
            }
        }
    }

    @Test
    fun `제목과 본문을 보이고 확인을 누르면 경고 진동을 내고 확인만 부른다`() {
        show()
        rule.onNodeWithText("모의고사를 나갈까요?").assertIsDisplayed()
        rule.onNodeWithText("진행은 저장됩니다.").assertIsDisplayed()

        rule.onNodeWithText("나가기").performClick()

        assertEquals(listOf("confirm"), calls)
        assertEquals(listOf(HapticEffect.Warning), played)
    }

    @Test
    fun `취소를 누르면 진동 없이 취소만 부른다`() {
        show()

        rule.onNodeWithText("취소").performClick()

        assertEquals(listOf("cancel"), calls)
        assertEquals(emptyList<HapticEffect>(), played)
    }

    private fun confirmWidth() = rule.onNodeWithTag("alert-confirm").getBoundsInRoot().let { it.right - it.left }

    @Test
    fun `나타날 때 작게 시작해 제 크기로 커진다`() {
        rule.mainClock.autoAdvance = false
        show()
        rule.mainClock.advanceTimeByFrame()
        val first = confirmWidth()
        rule.mainClock.advanceTimeBy(2_000)
        assertTrue(first < confirmWidth())
    }

    @Test
    fun `동작 줄이기면 첫 프레임부터 제 크기다`() {
        rule.mainClock.autoAdvance = false
        show(reduceMotion = true)
        rule.mainClock.advanceTimeByFrame()
        val first = confirmWidth()
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals(confirmWidth(), first)
    }
}

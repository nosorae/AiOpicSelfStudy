package io.github.nosorae.aiopic.ui.featureexam

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 설정의 서베이는 바꾼 답이 있을 때만 나가기 전에 묻는다. 저장하지 않고 나가면 바꾼 답은 버려지기 때문이다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class SurveyScreenTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private var closed = 0

    private fun show(state: SurveyUiState) = rule.setContent {
        MontageTheme { SurveyScreen(state = state, onIntent = {}, onClose = { closed++ }) }
    }

    @Test
    fun `바꾼 답이 없으면 나가기 X 를 누를 때 묻지 않고 닫는다`() {
        show(SurveySamples.unchanged)

        rule.onNodeWithContentDescription("나가기").performClick()

        assertEquals(1, closed)
    }

    @Test
    fun `바꾼 답이 있으면 저장되지 않는다고 묻고 취소하면 머물고 나가기를 누르면 닫는다`() {
        show(SurveySamples.edited)

        rule.onNodeWithContentDescription("나가기").performClick()
        rule.onNodeWithText("서베이를 나갈까요?").assertIsDisplayed()
        rule.onNodeWithText("바꾼 답은 저장되지 않습니다. 저장하려면 마지막 묶음에서 저장하기를 누르세요.").assertIsDisplayed()
        rule.onNodeWithText("취소").performClick()
        assertEquals(0, closed)

        rule.onNodeWithContentDescription("나가기").performClick()
        rule.onNodeWithText("나가기").performClick()
        assertEquals(1, closed)
    }

    @Test
    fun `바꾼 답이 있을 때 시스템 뒤로가기를 누르면 확인 대화상자를 보인다`() {
        show(SurveySamples.edited)

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

        rule.onNodeWithText("서베이를 나갈까요?").assertIsDisplayed()
        assertEquals(0, closed)
    }
}

package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.ui.test.junit4.createComposeRule
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

/** 오래 걸릴 수 있는 버튼(Play, 다음, Next)이 처리 중에 진행 표시를 보이고 눌리지 않는지 본다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ExamScreenTest {
    @get:Rule val rule = createComposeRule()

    private val intents = mutableListOf<ExamIntent>()

    private fun show(state: ExamUiState) = rule.setContent {
        MontageTheme { ExamScreen(state = state, onIntent = { intents += it }) }
    }

    private fun assertBusyButtonIgnoresClick(hiddenText: String) {
        rule.onNodeWithText(hiddenText).assertDoesNotExist()
        rule.onNodeWithContentDescription("처리 중").performClick()

        assertEquals(emptyList<ExamIntent>(), intents)
    }

    @Test
    fun `질문을 받아 오고 읽는 동안 Play 는 진행 표시를 보이고 눌리지 않는다`() {
        show(ExamSamples.questionSpeaking)

        assertBusyButtonIgnoresClick("Play")
    }

    @Test
    fun `답변을 저장하는 동안 Next 는 진행 표시를 보이고 눌리지 않는다`() {
        show(ExamSamples.questionSaving)

        assertBusyButtonIgnoresClick("Next")
    }

    @Test
    fun `서베이를 저장하는 동안 다음은 진행 표시를 보이고 눌리지 않는다`() {
        show(ExamSamples.surveySaving)

        assertBusyButtonIgnoresClick("다음")
    }
}

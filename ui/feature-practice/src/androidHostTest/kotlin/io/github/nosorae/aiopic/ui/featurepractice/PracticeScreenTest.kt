package io.github.nosorae.aiopic.ui.featurepractice

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

/** 오래 걸릴 수 있는 버튼(Play, Next)이 처리 중에 진행 표시를 보이고 눌리지 않는지 본다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class PracticeScreenTest {
    @get:Rule val rule = createComposeRule()

    private val intents = mutableListOf<PracticeIntent>()

    private fun show(state: PracticeUiState) = rule.setContent {
        MontageTheme { PracticeScreen(state = state, onIntent = { intents += it }) }
    }

    private fun assertBusyButtonIgnoresClick(hiddenText: String) {
        rule.onNodeWithText(hiddenText).assertDoesNotExist()
        rule.onNodeWithContentDescription("처리 중").performClick()

        assertEquals(emptyList<PracticeIntent>(), intents)
    }

    @Test
    fun `질문을 고르는 동안 Play 는 진행 표시를 보이고 눌리지 않는다`() {
        show(PracticeSamples.loading)

        assertBusyButtonIgnoresClick("Play")
    }

    @Test
    fun `질문을 읽는 동안 Play 는 진행 표시를 보이고 눌리지 않는다`() {
        show(PracticeSamples.speaking)

        assertBusyButtonIgnoresClick("Play")
    }

    @Test
    fun `답변을 저장하는 동안 Next 는 진행 표시를 보이고 눌리지 않는다`() {
        show(PracticeSamples.saving)

        assertBusyButtonIgnoresClick("Next")
    }
}

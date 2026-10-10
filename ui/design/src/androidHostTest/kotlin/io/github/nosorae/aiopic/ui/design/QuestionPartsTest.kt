package io.github.nosorae.aiopic.ui.design

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import io.github.nosorae.aiopic.libs.haptics.api.Haptics
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

/** 문항 화면 공통 부품. 단계 표시가 지난 단계와 지금 단계를 알리는지, 질문을 듣는 동안 Play 가 눌리지 않는지, 다시 듣기가 5초 뒤 사라지는지, Next 가 켜질 때 진동하는지 본다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class QuestionPartsTest {
    @get:Rule val rule = createComposeRule()

    private fun state(value: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun `지난 단계는 끝남, 지금 단계는 지금으로 읽힌다`() {
        rule.setContent { MontageTheme(reduceMotion = true) { MontageAnswerSteps(current = AnswerStep.Answer, withFeedback = true) } }

        rule.onNodeWithText("듣기").assert(state("끝남"))
        rule.onNodeWithText("답하기").assert(state("지금"))
        rule.onNodeWithText("피드백").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun `피드백이 없는 화면은 듣기와 답하기만 보인다`() {
        rule.setContent { MontageTheme(reduceMotion = true) { MontageAnswerSteps(current = AnswerStep.Listen, withFeedback = false) } }

        rule.onNodeWithText("답하기").assertExists()
        rule.onNodeWithText("피드백").assertDoesNotExist()
    }

    @Test
    fun `질문을 듣는 동안 Play 는 처리 중으로 읽히고 눌리지 않는다`() {
        var clicks = 0
        rule.setContent { MontageTheme(reduceMotion = true) { MontagePlayButton(text = "Play", busy = true, onClick = { clicks++ }) } }

        rule.onNodeWithText("Play").assertDoesNotExist()
        rule.onNodeWithContentDescription("처리 중").performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun `Play 는 글자로 읽히고 누르면 한 번 불린다`() {
        var clicks = 0
        rule.setContent { MontageTheme(reduceMotion = true) { MontagePlayButton(text = "Play", busy = false, onClick = { clicks++ }) } }

        rule.onNodeWithText("Play").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `Next 가 켜지는 순간 누름 진동을 한 번 내고 처음 그릴 때는 내지 않는다`() {
        val played = mutableListOf<HapticEffect>()
        var canNext by mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalHaptics provides Haptics { played += it }) {
                MontageTheme(reduceMotion = true) {
                    MontageRecordingButtons(9.seconds, 5.seconds, 10.seconds, canReplay = false, canNext = canNext, saving = false, onReplay = {}, onNext = {})
                }
            }
        }
        rule.waitForIdle()
        assertEquals(emptyList<HapticEffect>(), played)

        canNext = true
        rule.waitForIdle()

        assertEquals(listOf(HapticEffect.Tap), played)
    }

    @Test
    fun `다시 듣기는 쓸 수 있는 동안 보이고 5초가 끝나면 사라진다`() {
        var recorded by mutableStateOf(3.seconds)
        var canReplay by mutableStateOf(true)
        rule.setContent {
            MontageTheme(reduceMotion = true) {
                MontageRecordingButtons(recorded, 5.seconds, 10.seconds, canReplay = canReplay, canNext = false, saving = false, onReplay = {}, onNext = {})
            }
        }
        rule.onNodeWithText("다시 듣기").assertExists()

        recorded = 5.seconds
        canReplay = false
        rule.waitForIdle()

        rule.onNodeWithText("다시 듣기").assertDoesNotExist()
    }
}

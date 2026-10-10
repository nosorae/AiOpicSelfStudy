package io.github.nosorae.aiopic.ui.featurepractice

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import io.github.nosorae.aiopic.libs.haptics.api.Haptics
import io.github.nosorae.aiopic.ui.design.LocalHaptics
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 오래 걸릴 수 있는 버튼(Play, Next)이 처리 중에 진행 표시를 보이고 눌리지 않는지, 녹음 오류마다 고칠 버튼이 보이는지 본다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class PracticeScreenTest {
    private companion object {
        const val HINT = "질문이 끝나면 바로 녹음돼요. 다시 듣기는 5초 안에 한 번, Next 는 10초 뒤에 켜져요."
    }

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val intents = mutableListOf<PracticeIntent>()

    private var settingsOpened = 0

    // 녹음 중 목소리 띠는 끝없이 움직여 시계가 흐르면 화면이 쉬지 않는다. 움직임을 보지 않는 테스트는 동작 줄이기로 띠를 멈춘다.
    private fun show(state: PracticeUiState) = rule.setContent {
        MontageTheme(reduceMotion = true) { PracticeScreen(state = state, onIntent = { intents += it }, onOpenSettings = { settingsOpened++ }) }
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

    @Test
    fun `녹음을 저장하지 못했으면 저장 공간 안내와 다시 녹음 버튼이 보이고 누르면 다시 녹음한다`() {
        show(PracticeSamples.recordingError)

        rule.onNodeWithText("녹음을 저장하지 못했습니다. 기기 저장 공간을 확인하고 다시 녹음을 누르세요.").assertExists()
        rule.onNodeWithText("다시 녹음").performClick()

        assertEquals(listOf<PracticeIntent>(PracticeIntent.PlayQuestion), intents)
    }

    @Test
    fun `마이크 권한이 꺼져 있으면 설정 열기 버튼이 보이고 누르면 설정을 연다`() {
        show(PracticeSamples.micError)

        rule.onNodeWithText("설정 열기").performClick()

        assertEquals(1, settingsOpened)
    }

    @Test
    fun `녹음이 시작되면 녹음 시작 진동, 멈추면 녹음 끝 진동을 내고 처음 그릴 때는 내지 않는다`() {
        val played = mutableListOf<HapticEffect>()
        var state by mutableStateOf(PracticeSamples.ready)
        rule.setContent {
            CompositionLocalProvider(LocalHaptics provides Haptics { played += it }) {
                MontageTheme(reduceMotion = true) { PracticeScreen(state = state, onIntent = {}) }
            }
        }
        rule.waitForIdle()
        assertEquals(emptyList<HapticEffect>(), played)

        state = PracticeSamples.replayable
        rule.waitForIdle()
        state = PracticeSamples.speaking
        rule.waitForIdle()

        assertEquals(listOf(HapticEffect.RecordStart, HapticEffect.RecordStop), played)
    }

    private fun step(label: String, state: String) = rule.onNodeWithText(label).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))

    @Test
    fun `규칙 문단 대신 듣기, 답하기, 피드백 단계가 보이고 Play 전에는 듣기가 지금 단계다`() {
        show(PracticeSamples.ready)

        step("듣기", "지금")
        rule.onNodeWithText("피드백").assertExists()
        rule.onNodeWithText("다시 듣기는 녹음 시작 5초 안에 한 번만 됩니다. Next 는 녹음 10초 뒤에 켜집니다.").assertDoesNotExist()
        rule.onNodeWithText("약점 점수가 낮은 유형에서 질문 하나를 냅니다. 답을 내면 피드백을 바로 보여 줍니다.").assertDoesNotExist()
    }

    @Test
    fun `녹음 중에는 답하기가 지금 단계이고 짧은 안내는 사라진다`() {
        show(PracticeSamples.replayable)

        step("듣기", "끝남")
        step("답하기", "지금")
        rule.onNodeWithText(HINT).assertDoesNotExist()
    }

    @Test
    fun `Play 전에는 규칙을 짧은 안내 한 줄로 보인다`() {
        show(PracticeSamples.ready)

        rule.onNodeWithText(HINT).assertExists()
    }

    @Test
    fun `답변을 저장하는 동안 피드백이 지금 단계다`() {
        show(PracticeSamples.saving)

        step("답하기", "끝남")
        step("피드백", "지금")
    }

    @Test
    fun `녹음 중 나가기를 누르면 피드백 단계가 되지 않는다`() {
        show(PracticeSamples.exiting)

        step("답하기", "지금")
        rule.onNodeWithText("피드백").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "지금").not())
    }

    @Test
    fun `왜 이 질문인지 약한 유형 이름과 점수가 보인다`() {
        show(PracticeSamples.ready)

        rule.onNodeWithText("롤플레이 해결", substring = true).assertExists()
        rule.onNodeWithText("42점").assertExists()
    }

    @Test
    fun `분석한 답변이 없으면 모든 유형에서 골랐다고 보인다`() {
        show(PracticeSamples.ready.copy(weakness = null))

        rule.onNodeWithText("모든 유형에서 골랐어요").assertExists()
    }

    @Test
    fun `동작 줄이기면 Next 가 켜지기까지 남은 초를 글자로 보인다`() {
        rule.setContent { MontageTheme(reduceMotion = true) { PracticeScreen(state = PracticeSamples.replayable, onIntent = {}) } }

        rule.onNodeWithText("Next (7초)").assertExists()
    }

    @Test
    fun `녹음 중이 아니면 나가기 X 를 누를 때 묻지 않고 나간다`() {
        show(PracticeSamples.ready)

        rule.onNodeWithContentDescription("나가기").performClick()

        assertEquals(listOf<PracticeIntent>(PracticeIntent.ExitPractice), intents)
    }

    @Test
    fun `녹음 중에 나가기 X 를 누르면 답변이 저장되지 않는다고 묻고 아직 나가지 않는다`() {
        show(PracticeSamples.replayable)

        rule.onNodeWithContentDescription("나가기").performClick()

        rule.onNodeWithText("연습을 나갈까요?").assertIsDisplayed()
        rule.onNodeWithText("지금 녹음한 답변은 저장되지 않고 피드백도 받지 않습니다.").assertIsDisplayed()
        assertEquals(emptyList<PracticeIntent>(), intents)
    }

    @Test
    fun `확인 대화상자에서 취소를 누르면 연습에 머물고 나가기를 누르면 나간다`() {
        show(PracticeSamples.replayable)

        rule.onNodeWithContentDescription("나가기").performClick()
        rule.onNodeWithText("취소").performClick()
        rule.onNodeWithText("연습을 나갈까요?").assertDoesNotExist()
        assertEquals(emptyList<PracticeIntent>(), intents)

        rule.onNodeWithContentDescription("나가기").performClick()
        rule.onNodeWithText("나가기").performClick()
        assertEquals(listOf<PracticeIntent>(PracticeIntent.ExitPractice), intents)
    }

    @Test
    fun `녹음 중에 시스템 뒤로가기를 누르면 확인 대화상자를 보인다`() {
        show(PracticeSamples.replayable)

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

        rule.onNodeWithText("연습을 나갈까요?").assertIsDisplayed()
        assertEquals(emptyList<PracticeIntent>(), intents)
    }
}

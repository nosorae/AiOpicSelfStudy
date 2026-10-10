package io.github.nosorae.aiopic.ui.featurepractice

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
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
    @get:Rule val rule = createComposeRule()

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

    @Test
    fun `동작 줄이기면 Next 가 켜지기까지 남은 초를 글자로 보인다`() {
        rule.setContent { MontageTheme(reduceMotion = true) { PracticeScreen(state = PracticeSamples.replayable, onIntent = {}) } }

        rule.onNodeWithText("Next (7초)").assertExists()
    }
}

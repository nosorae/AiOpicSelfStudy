package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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

/** 오래 걸릴 수 있는 버튼(Play, 다음, Next)이 처리 중에 진행 표시를 보이고 눌리지 않는지, 녹음 오류마다 고칠 버튼이 보이는지 본다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ExamScreenTest {
    private companion object {
        const val HINT = "질문이 끝나면 바로 녹음돼요. 다시 듣기는 5초 안에 한 번, Next 는 10초 뒤에 켜져요."
    }

    @get:Rule val rule = createComposeRule()

    private val intents = mutableListOf<ExamIntent>()

    private var settingsOpened = 0

    // 녹음 중 목소리 띠는 끝없이 움직여 시계가 흐르면 화면이 쉬지 않는다. 움직임을 보지 않는 테스트는 동작 줄이기로 띠를 멈춘다.
    private fun show(state: ExamUiState) = rule.setContent {
        MontageTheme(reduceMotion = true) { ExamScreen(state = state, onIntent = { intents += it }, onOpenSettings = { settingsOpened++ }) }
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

    @Test
    fun `F2_2 서베이 질문은 보기를 누르면 그 보기를 고른다는 Intent 하나만 보낸다`() {
        show(ExamSamples.survey)

        rule.onNodeWithText("가르치는 일").performClick()

        assertEquals(listOf<ExamIntent>(ExamIntent.ChooseSurveyOption("teacher")), intents)
    }

    @Test
    fun `서베이 주제 칩을 누르면 선택 진동을 내고 주제를 바꾼다`() {
        val played = mutableListOf<HapticEffect>()
        rule.setContent {
            CompositionLocalProvider(LocalHaptics provides Haptics { played += it }) {
                MontageTheme { ExamScreen(state = ExamSamples.surveyGroup, onIntent = { intents += it }) }
            }
        }

        rule.onNodeWithText("캠핑하기").performClick()

        assertEquals(listOf(HapticEffect.Selection), played)
        assertEquals(listOf<ExamIntent>(ExamIntent.ToggleTopic("캠핑하기")), intents)
    }

    @Test
    fun `F2_2 마지막 묶음에서 전체가 12개보다 적으면 다음이 꺼져 있고 앞 묶음으로 돌아갈 수 있다`() {
        show(ExamSamples.surveyShort)

        rule.onNodeWithText("다음").assertIsNotEnabled()
        rule.onNodeWithText("이전").performClick()

        assertEquals(listOf<ExamIntent>(ExamIntent.PreviousSurveyScreen), intents)
    }

    @Test
    fun `F2_2 첫 묶음 화면부터 전체 선택 수가 스크롤 밖 버튼 위에 있어 큰 글자에서도 아래로 밀리지 않는다`() {
        show(ExamSamples.surveyGroup)

        rule.onNodeWithTag("survey-total").assertIsDisplayed().assertTextEquals("3")
        rule.onNode(hasTestTag("survey-total") and hasAnyAncestor(hasScrollAction())).assertDoesNotExist()
    }

    @Test
    fun `F2_2 묶음 최소를 채우면 그 조건에만 체크가 붙고 전체 12개가 남았으면 전체에는 체크가 없다`() {
        show(ExamSamples.surveyGroup)

        rule.onNodeWithText("이 묶음 2개 이상").assertIsDisplayed()
        rule.onNodeWithText("전체 12개 이상").assertIsDisplayed()
        rule.onNodeWithTag("survey-group-met").assertIsDisplayed()
        rule.onNodeWithTag("survey-total-met").assertDoesNotExist()
    }

    @Test
    fun `F2_2 전체 12개에 닿으면 전체 조건에도 체크가 붙는다`() {
        show(ExamSamples.surveyReady)

        rule.onNodeWithTag("survey-group-met").assertIsDisplayed()
        rule.onNodeWithTag("survey-total-met").assertIsDisplayed()
    }

    @Test
    fun `녹음을 저장하지 못했으면 저장 공간 안내와 다시 녹음 버튼이 보이고 누르면 같은 문항을 다시 녹음한다`() {
        show(ExamSamples.recordingError)

        rule.onNodeWithText("녹음을 저장하지 못했습니다. 기기 저장 공간을 확인하고 다시 녹음을 누르세요.").assertExists()
        rule.onNodeWithText("다시 녹음").performClick()

        assertEquals(listOf<ExamIntent>(ExamIntent.PlayQuestion), intents)
    }

    @Test
    fun `마이크 권한이 꺼져 있으면 설정 열기 버튼이 보이고 누르면 설정을 연다`() {
        show(ExamSamples.micError)

        rule.onNodeWithText("설정 열기").performClick()

        assertEquals(1, settingsOpened)
        rule.onNodeWithText("Play").assertExists()
    }

    private val played = mutableListOf<HapticEffect>()

    private fun showWithHaptics(state: ExamUiState, reduceMotion: Boolean = true) = rule.setContent {
        CompositionLocalProvider(LocalHaptics provides Haptics { played += it }) {
            MontageTheme(reduceMotion = reduceMotion) { ExamScreen(state = state, onIntent = { intents += it }) }
        }
    }

    @Test
    fun `녹음이 시작되면 녹음 시작 진동, 멈추면 녹음 끝 진동을 내고 처음 그릴 때는 내지 않는다`() {
        var state by mutableStateOf(ExamSamples.questionReplayable)
        rule.setContent {
            CompositionLocalProvider(LocalHaptics provides Haptics { played += it }) {
                MontageTheme(reduceMotion = true) { ExamScreen(state = state, onIntent = {}) }
            }
        }
        rule.waitForIdle()
        assertEquals(emptyList<HapticEffect>(), played)

        state = ExamSamples.questionSpeaking
        rule.waitForIdle()
        state = ExamSamples.questionReplayable
        rule.waitForIdle()

        assertEquals(listOf(HapticEffect.RecordStop, HapticEffect.RecordStart), played)
    }

    @Test
    fun `Play 를 누르면 누름 진동을 내고 질문을 튼다`() {
        showWithHaptics(ExamSamples.questionReady)

        rule.onNodeWithTag("exam-play").performClick()

        assertEquals(listOf(HapticEffect.Tap), played)
        assertEquals(listOf<ExamIntent>(ExamIntent.PlayQuestion), intents)
    }

    @Test
    fun `수준을 고르면 선택 진동을 내고 다음을 누르면 누름 진동을 낸다`() {
        showWithHaptics(ExamSamples.level)

        rule.onNodeWithTag("exam-choice-1").performClick()
        rule.onNodeWithTag("exam-next-step").performClick()

        assertEquals(listOf(HapticEffect.Selection, HapticEffect.Tap), played)
        assertEquals(listOf(ExamIntent.SelectLevel(2), ExamIntent.SubmitLevel), intents)
    }

    @Test
    fun `재조정 카드를 고르면 선택 진동을 내고 그 난이도를 고른다`() {
        showWithHaptics(ExamSamples.recalibrate)

        rule.onNodeWithText("어려운 질문").performClick()

        assertEquals(listOf(HapticEffect.Selection), played)
        assertEquals(listOf<ExamIntent>(ExamIntent.SelectRecalibration(io.github.nosorae.aiopic.engine.session.model.Recalibration.Harder)), intents)
    }

    @Test
    fun `문항 진행 점은 지난 문항까지 차 있다`() {
        show(ExamSamples.questionReplayable)

        rule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(2f, 0f..15f, 15))).assertExists()
    }

    @Test
    fun `시험 문항은 피드백 없이 듣기와 답하기 단계만 보이고 녹음 중에는 답하기가 지금 단계다`() {
        show(ExamSamples.questionReplayable)

        rule.onNodeWithText("듣기").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "끝남"))
        rule.onNodeWithText("답하기").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "지금"))
        rule.onNodeWithText("피드백").assertDoesNotExist()
    }

    @Test
    fun `규칙은 장비 점검에서만 짧은 안내 한 줄로 보이고 문항에서는 그림이 대신한다`() {
        show(ExamSamples.check)
        rule.onNodeWithText(HINT).assertExists()
        rule.onNodeWithText("다시 듣기는 녹음 시작 5초 안에 한 번만 됩니다. Next 는 녹음 10초 뒤에 켜집니다.").assertDoesNotExist()
    }

    @Test
    fun `문항 화면에는 규칙 글이 없다`() {
        show(ExamSamples.questionReady)

        rule.onNodeWithText(HINT).assertDoesNotExist()
        rule.onNodeWithText("다시 듣기는 녹음 시작 5초 안에 한 번만 됩니다. Next 는 녹음 10초 뒤에 켜집니다.").assertDoesNotExist()
    }

    @Test
    fun `질문을 만드는 동안 진행 문구가 무엇을 만드는지 알린다`() {
        show(ExamSamples.questionLoading)

        rule.onNodeWithTag("exam-phase").assertTextEquals("고른 주제로 2~7번 질문을 만들고 있어요")
    }

    @Test
    fun `동작 줄이기면 Next 가 켜지기까지 남은 초를 글자로 보인다`() {
        showWithHaptics(ExamSamples.questionReplayable, reduceMotion = true)

        rule.onNodeWithText("Next (7초)").assertExists()
    }

    @Test
    fun `나가기 X 를 누르면 확인 대화상자를 보이고 아직 나가지 않는다`() {
        show(ExamSamples.questionReplayable)

        rule.onNodeWithContentDescription("나가기").performClick()

        rule.onNodeWithText("모의고사을 나갈까요?").assertIsDisplayed()
        rule.onNodeWithText("진행과 녹음은 저장되고 나가 있는 동안 40분 타이머가 멈춥니다. 모의고사 탭의 이어 보기로 여기서부터 이어서 할 수 있습니다.").assertIsDisplayed()
        assertEquals(emptyList<ExamIntent>(), intents)
    }

    @Test
    fun `확인 대화상자에서 취소를 누르면 시험에 머문다`() {
        show(ExamSamples.questionReplayable)

        rule.onNodeWithContentDescription("나가기").performClick()
        rule.onNodeWithText("취소").performClick()

        rule.onNodeWithText("모의고사을 나갈까요?").assertDoesNotExist()
        assertEquals(emptyList<ExamIntent>(), intents)
    }

    @Test
    fun `확인 대화상자에서 나가기를 누르면 시험을 나간다`() {
        show(ExamSamples.questionReplayable)

        rule.onNodeWithContentDescription("나가기").performClick()
        rule.onNodeWithText("나가기").performClick()

        assertEquals(listOf<ExamIntent>(ExamIntent.ExitExam), intents)
    }

    @Test
    fun `서베이 중에 나가면 이번에 고른 답이 저장되지 않는다고 알린다`() {
        show(ExamSamples.survey)

        rule.onNodeWithContentDescription("나가기").performClick()

        rule.onNodeWithText("이번에 고른 서베이 답은 저장되지 않습니다. 모의고사 탭의 이어 보기로 서베이부터 다시 할 수 있습니다.").assertIsDisplayed()
    }

    @Test
    fun `40분이 시작되기 전에 나가면 타이머 이야기 없이 진행이 저장된다고 알린다`() {
        show(ExamSamples.level)

        rule.onNodeWithContentDescription("나가기").performClick()

        rule.onNodeWithText("진행은 저장됩니다. 모의고사 탭의 이어 보기로 여기서부터 이어서 할 수 있습니다.").assertIsDisplayed()
    }
}

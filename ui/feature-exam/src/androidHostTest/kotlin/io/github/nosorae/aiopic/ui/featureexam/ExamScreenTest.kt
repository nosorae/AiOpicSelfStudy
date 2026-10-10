package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
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
    @get:Rule val rule = createComposeRule()

    private val intents = mutableListOf<ExamIntent>()

    private var settingsOpened = 0

    private fun show(state: ExamUiState) = rule.setContent {
        MontageTheme { ExamScreen(state = state, onIntent = { intents += it }, onOpenSettings = { settingsOpened++ }) }
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
}

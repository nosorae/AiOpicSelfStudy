package io.github.nosorae.aiopic.ui.featurehome

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 모의고사 탭(S3). 이어 보기, 시작, 다시 보기 버튼이 어느 회차를 넘기는지 본다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class HomeScreenTest {
    @get:Rule val rule = createComposeRule()

    private val opened = mutableListOf<String?>()
    private val reviewed = mutableListOf<String>()

    // 등급을 기다리는 줄의 물결이 끝없이 돌아서 시계를 손으로 넘긴다. 자동으로 두면 화면이 쉬지 않아 테스트가 기다리다 끝난다.
    private fun show(state: HomeUiState) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MontageTheme { HomeScreen(state = state, onExamClick = { opened += it }, onSessionClick = { reviewed += it }) }
        }
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    @Test
    fun `회차를 읽는 동안 시작 버튼은 진행 표시를 보이고 눌리지 않는다`() {
        show(HomeSamples.loading)

        rule.onNodeWithContentDescription("처리 중").performClick()

        assertEquals(emptyList<String?>(), opened)
    }

    @Test
    fun `F3 이어 보기를 누르면 진행 중인 회차 id 를 넘긴다`() {
        show(HomeSamples.history)

        rule.onNodeWithText("이어 보기").performClick()

        assertEquals(listOf<String?>("d"), opened)
    }

    @Test
    fun `진행 중인 회차가 있으면 새로 시작은 확인을 받고 나서 id 없이 넘기고 이어 보기가 더 위에 있다`() {
        show(HomeSamples.history)

        val resumeTop = rule.onNodeWithTag("exam-resume").fetchSemanticsNode().boundsInRoot.top
        val startTop = rule.onNodeWithTag("exam-start").fetchSemanticsNode().boundsInRoot.top
        rule.onNodeWithTag("exam-start").performClick()
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)

        assertTrue(resumeTop < startTop)
        assertEquals(emptyList<String?>(), opened)
        rule.onNodeWithText("새 모의고사를 시작할까요?").assertIsDisplayed()
        rule.onNodeWithText("미완료", substring = true).assertIsDisplayed()

        rule.onAllNodesWithText("새로 시작").onLast().performClick()
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)

        assertEquals(listOf<String?>(null), opened)
        rule.onNodeWithText("새 모의고사를 시작할까요?").assertDoesNotExist()
    }

    @Test
    fun `새로 시작 확인에서 취소하면 아무 데도 가지 않는다`() {
        show(HomeSamples.history)

        rule.onNodeWithTag("exam-start").performClick()
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        rule.onNodeWithText("취소").performClick()
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)

        assertEquals(emptyList<String?>(), opened)
        rule.onNodeWithText("새 모의고사를 시작할까요?").assertDoesNotExist()
    }

    @Test
    fun `진행 중인 회차가 없으면 이어 보기 없이 시작만 있다`() {
        show(HomeSamples.empty)

        rule.onNodeWithTag("exam-resume").assertDoesNotExist()
        rule.onNodeWithText("시작").performClick()

        assertEquals(listOf<String?>(null), opened)
    }

    @Test
    fun `#230 최근 회차는 셋까지 보이고 누른 회차의 리포트를 연다`() {
        show(HomeSamples.history)

        rule.onNodeWithText("모의고사 3회차").performScrollTo().performClick()

        assertEquals(listOf("b"), reviewed)
        rule.onNodeWithText("모의고사 1회차").assertDoesNotExist()
    }

    @Test
    fun `#230 시작 전에 서베이, 난이도 선택, 문항 수와 시간을 한눈에 보인다`() {
        show(HomeSamples.empty)

        rule.onNodeWithText("서베이").assertExists()
        rule.onNodeWithText("관심 주제를 12개 이상 고릅니다").assertExists()
        rule.onNodeWithText("난이도 선택").assertExists()
        rule.onNodeWithText("12~15문항 답하기").assertExists()
        rule.onNodeWithText("40분", substring = true).assertExists()
    }

    @Test
    fun `끝낸 회차가 없으면 다시 보기 대신 안내를 보인다`() {
        show(HomeSamples.empty)

        rule.onNodeWithTag("exam-review").assertDoesNotExist()
        rule.onNodeWithText("아직 마친 회차가 없습니다", substring = true).assertExists()
    }

    @Test
    fun `F3 이어 볼 회차가 있으면 몇 문항을 마쳤는지 보인다`() {
        show(HomeSamples.history)

        rule.onNodeWithText("15문항 중 4문항을 마쳤습니다").assertExists()
    }

    private companion object {
        const val SETTLE_MILLIS = 1_000L
    }
}

package io.github.nosorae.aiopic.ui.featurehome

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private fun show(state: HomeUiState) = rule.setContent {
        MontageTheme { HomeScreen(state = state, onExamClick = { opened += it }, onSessionClick = { reviewed += it }) }
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
    fun `진행 중인 회차가 있어도 새로 시작은 id 없이 넘기고 이어 보기가 더 위에 있다`() {
        show(HomeSamples.history)

        val resumeTop = rule.onNodeWithTag("exam-resume").fetchSemanticsNode().boundsInRoot.top
        val startTop = rule.onNodeWithTag("exam-start").fetchSemanticsNode().boundsInRoot.top
        rule.onNodeWithText("새로 시작").performClick()

        assertTrue(resumeTop < startTop)
        assertEquals(listOf<String?>(null), opened)
    }

    @Test
    fun `진행 중인 회차가 없으면 이어 보기 없이 시작만 있다`() {
        show(HomeSamples.empty)

        rule.onNodeWithTag("exam-resume").assertDoesNotExist()
        rule.onNodeWithText("시작").performClick()

        assertEquals(listOf<String?>(null), opened)
    }

    @Test
    fun `다시 보기는 가장 최근에 끝낸 회차의 리포트를 연다`() {
        show(HomeSamples.history)

        rule.onNodeWithText("다시 보기").performClick()

        assertEquals(listOf("c"), reviewed)
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
}

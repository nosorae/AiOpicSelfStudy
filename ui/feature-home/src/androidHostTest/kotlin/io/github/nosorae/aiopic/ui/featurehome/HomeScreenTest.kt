package io.github.nosorae.aiopic.ui.featurehome

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

/** 회차를 읽기 전에는 시작인지 이어 보기인지 몰라서 시험 버튼이 진행 표시를 보이고 눌리지 않는다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class HomeScreenTest {
    @get:Rule val rule = createComposeRule()

    private val opened = mutableListOf<String?>()

    private fun show(state: HomeUiState) = rule.setContent {
        MontageTheme { HomeScreen(state = state, onExamClick = { opened += it }, onPracticeClick = {}, onSessionClick = {}) }
    }

    @Test
    fun `회차를 읽는 동안 시험 버튼은 진행 표시를 보이고 눌리지 않는다`() {
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
    fun `진행 중인 회차가 없으면 시작을 눌렀을 때 id 없이 넘긴다`() {
        show(HomeSamples.empty)

        rule.onNodeWithText("모의시험 시작").performClick()

        assertEquals(listOf<String?>(null), opened)
    }
}

package io.github.nosorae.aiopic.ui.featurereport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import io.github.nosorae.aiopic.libs.haptics.api.Haptics
import io.github.nosorae.aiopic.ui.design.LocalHaptics
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

/**
 * 기다린 결과가 화면을 보는 중에 나오면 축하한다. 회차 등급은 구슬이 멈춘 뒤 큰 축하, 답변 분석은 점수 옆 작은 축하다.
 * 이미 나와 있는 결과를 다시 열 때는 상태가 바뀐 게 아니라서 축하하지 않는다. 교정에 틀림을 누르면 선택 진동을 낸다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ReportHapticsTest {
    @get:Rule val rule = createComposeRule()

    private val played = mutableListOf<HapticEffect>()

    /** 같은 답변이 분석 전인 상태. */
    private val pending = ReportSamples.answerEvaluated.let { it.copy(answer = it.answer?.copy(analysis = AnswerAnalysis.Pending)) }

    @Test
    fun `등급을 기다리던 중에 등급이 나오면 구슬이 멈춘 뒤 큰 축하를 한다`() {
        var state by mutableStateOf(ReportSamples.grading)
        show(dark = false) { SessionReportScreen(state = state, onOpenAnswer = {}, onRetryClick = {}) }

        change { state = ReportSamples.mixedModels }
        rule.mainClock.advanceTimeBy(1_500)

        assertEquals(listOf(HapticEffect.Celebrate), played)
    }

    @Test fun session_gradeCelebration() = gradeCelebration(dark = false)

    @Test fun session_gradeCelebration_dark() = gradeCelebration(dark = true)

    @Test fun answer_analysisCelebration() = analysisCelebration(dark = false)

    @Test fun answer_analysisCelebration_dark() = analysisCelebration(dark = true)

    /** 등급이 나오고 1.5초 뒤, 구슬이 멈추고 축하 조각이 터지는 중인 장면. */
    private fun gradeCelebration(dark: Boolean) {
        var state by mutableStateOf(ReportSamples.grading)
        show(dark) { SessionReportScreen(state = state, onOpenAnswer = {}, onRetryClick = {}) }
        change { state = ReportSamples.mixedModels }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onRoot().captureRoboImage()
    }

    /** 분석이 끝나고 0.3초 뒤, 점수 둘레로 불꽃이 퍼지는 중인 장면. */
    private fun analysisCelebration(dark: Boolean) {
        var state by mutableStateOf(pending)
        show(dark) { AnswerReportScreen(state = state, onIntent = {}) }
        change { state = ReportSamples.answerEvaluated }
        rule.mainClock.advanceTimeBy(300)
        rule.onRoot().captureRoboImage()
    }

    @Test
    fun `이미 등급이 나온 회차를 열면 축하하지 않는다`() {
        show(dark = false) { SessionReportScreen(state = ReportSamples.mixedModels, onOpenAnswer = {}, onRetryClick = {}) }

        rule.mainClock.advanceTimeBy(3_000)

        assertEquals(emptyList<HapticEffect>(), played)
    }

    @Test
    fun `답변 분석을 기다리던 중에 끝나면 점수 옆에 작은 축하를 한다`() {
        var state by mutableStateOf(pending)
        show(dark = false) { AnswerReportScreen(state = state, onIntent = {}) }

        change { state = ReportSamples.answerEvaluated }
        rule.mainClock.advanceTimeBy(700)

        assertEquals(listOf(HapticEffect.Success), played)
    }

    @Test
    fun `이미 분석된 답변을 열면 축하하지 않는다`() {
        show(dark = false) { AnswerReportScreen(state = ReportSamples.answerEvaluated, onIntent = {}) }

        rule.mainClock.advanceTimeBy(3_000)

        assertEquals(emptyList<HapticEffect>(), played)
    }

    @Test
    fun `F8_1 교정에 틀림을 누르면 선택 진동을 낸다`() {
        show(dark = false) { AnswerReportScreen(state = ReportSamples.answerEvaluated, onIntent = {}) }

        rule.onAllNodesWithText("틀림")[0].performScrollTo().performClick()

        assertEquals(listOf(HapticEffect.Selection), played)
    }

    private fun show(dark: Boolean, screen: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MontageTheme(darkTheme = dark) { CompositionLocalProvider(LocalHaptics provides Haptics { played += it }) { screen() } }
        }
        rule.mainClock.advanceTimeBy(100)
    }

    private fun change(block: () -> Unit) {
        block()
        Snapshot.sendApplyNotifications()
    }
}

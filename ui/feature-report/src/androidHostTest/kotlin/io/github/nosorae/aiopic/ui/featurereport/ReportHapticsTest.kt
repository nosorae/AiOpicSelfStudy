package io.github.nosorae.aiopic.ui.featurereport

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
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
import kotlin.test.assertTrue

/**
 * 기다린 결과가 화면을 보는 중에 나오면 축하한다. 회차 등급은 구슬이 멈춘 뒤 큰 축하, 답변 분석은 앱에서 처음 끝난 분석일 때만 점수 옆 작은 축하다.
 * 앞 회차보다 오른 등급을 처음 열 때도 큰 축하를 한다(#198). 동작 줄이기면 조각 없이 진동만 낸다.
 * 이미 나와 있는 결과를 다시 열 때는 상태가 바뀐 게 아니라서 축하하지 않는다. 교정에 틀림을 누르면 선택 진동을 낸다.
 * 최종 스크립트 복사는 누를 때 한 번만 울린다. 스크롤로 다시 그려도 또 울리지 않는다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ReportHapticsTest {
    @get:Rule val rule = createComposeRule()

    private val played = mutableListOf<HapticEffect>()

    /** 앱에서 처음 끝난 답변 분석. ViewModel 이 축하하라고 표시한 상태다(#198). */
    private val firstAnalysis = ReportSamples.answerEvaluated.copy(celebrateAnalysis = true)

    /** 같은 답변이 분석 전인 상태. */
    private val pending = ReportSamples.answerEvaluated.let { it.copy(answer = it.answer?.copy(analysis = AnswerAnalysis.Pending)) }

    @Test
    fun `등급을 기다리던 중에 등급이 나오면 구슬이 멈춘 뒤 큰 축하를 한다`() {
        var state by mutableStateOf(ReportSamples.grading)
        show(dark = false) { SessionReportScreen(state = state, onOpenAnswer = {}, onIntent = {}) }

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
        show(dark) { SessionReportScreen(state = state, onOpenAnswer = {}, onIntent = {}) }
        change { state = ReportSamples.mixedModels }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onRoot().captureRoboImage()
    }

    /** 분석이 끝나고 0.3초 뒤, 점수 둘레로 불꽃이 퍼지는 중인 장면. */
    private fun analysisCelebration(dark: Boolean) {
        var state by mutableStateOf(pending)
        show(dark) { AnswerReportScreen(state = state, onIntent = {}) }
        change { state = firstAnalysis }
        rule.mainClock.advanceTimeBy(300)
        rule.onRoot().captureRoboImage()
    }

    @Test
    fun `이미 등급이 나온 회차를 열면 축하하지 않는다`() {
        show(dark = false) { SessionReportScreen(state = ReportSamples.mixedModels, onOpenAnswer = {}, onIntent = {}) }

        rule.mainClock.advanceTimeBy(3_000)

        assertEquals(emptyList<HapticEffect>(), played)
    }

    @Test
    fun `첫 답변 분석이 보는 중에 끝나면 점수 옆에 작은 축하를 한다`() {
        var state by mutableStateOf(pending)
        show(dark = false) { AnswerReportScreen(state = state, onIntent = {}) }

        change { state = firstAnalysis }
        rule.mainClock.advanceTimeBy(700)

        assertEquals(listOf(HapticEffect.Success), played)
    }

    @Test
    fun `198 첫 분석이 아니면 보는 중에 끝나도 축하하지 않는다`() {
        var state by mutableStateOf(pending)
        show(dark = false) { AnswerReportScreen(state = state, onIntent = {}) }

        change { state = ReportSamples.answerEvaluated }
        rule.mainClock.advanceTimeBy(700)

        assertEquals(emptyList<HapticEffect>(), played)
    }

    @Config(fontScale = 2f)
    @Test
    fun answer_analysisCelebration_fontScale200() = analysisCelebration(dark = false)

    @Test fun session_gradeUp() = gradeUp(dark = false)

    @Test fun session_gradeUp_dark() = gradeUp(dark = true)

    @Config(fontScale = 2f)
    @Test
    fun session_gradeUp_fontScale200() = gradeUp(dark = false)

    /** 앞 회차보다 오른 등급을 처음 연 지 1.5초 뒤, 구슬이 멈추고 큰 축하가 터지는 중인 장면(#198). */
    private fun gradeUp(dark: Boolean) {
        show(dark) { SessionReportScreen(state = ReportSamples.gradeUp, onOpenAnswer = {}, onIntent = {}) }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onRoot().captureRoboImage()
    }

    @Test
    fun `198 앞 회차보다 오른 등급을 처음 열면 구슬이 멈춘 뒤 큰 축하를 한다`() {
        show(dark = false) { SessionReportScreen(state = ReportSamples.gradeUp, onOpenAnswer = {}, onIntent = {}) }
        rule.mainClock.advanceTimeBy(3_000)

        assertEquals(listOf(HapticEffect.Celebrate), played)
    }

    @Test
    fun `198 동작 줄이기면 오른 등급과 첫 분석 축하는 조각 없이 진동만 낸다`() {
        var state by mutableStateOf(pending)
        show(dark = false, reduceMotion = true) {
            Column {
                SessionReportScreen(state = ReportSamples.gradeUp, onOpenAnswer = {}, onIntent = {}, modifier = Modifier.weight(1f))
                AnswerReportScreen(state = state, onIntent = {}, modifier = Modifier.weight(1f))
            }
        }
        change { state = firstAnalysis }
        rule.mainClock.advanceTimeBy(1_300)
        val during = rule.onRoot().captureToImage().asAndroidBitmap()
        rule.mainClock.advanceTimeBy(2_000)

        assertTrue(during.sameAs(rule.onRoot().captureToImage().asAndroidBitmap()), "동작 줄이기인데 조각이 그려진다")
        assertEquals(setOf(HapticEffect.Celebrate, HapticEffect.Success), played.toSet())
        assertEquals(2, played.size)
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

    @Test
    fun `스크립트를 복사하고 위로 올렸다 다시 내려와도 성공 진동은 한 번이다`() {
        var state by mutableStateOf(ReportSamples.answerScripted.copy(scriptCopied = false))
        show(dark = false) {
            AnswerReportScreen(state = state, onIntent = { if (it == AnswerReportIntent.ScriptCopied) state = state.copy(scriptCopied = true) })
        }
        // 스크롤은 프레임이 돌아야 끝나서 이 시험만 시계를 저절로 흐르게 한다. 이 화면에 끝없이 도는 움직임은 없다.
        rule.mainClock.autoAdvance = true
        val list = rule.onNode(hasScrollToIndexAction())

        list.performScrollToNode(hasTestTag("script-copy"))
        rule.onNodeWithTag("script-copy").performClick()
        list.performScrollToIndex(0)
        rule.onNodeWithTag("script-copy").assertDoesNotExist()
        list.performScrollToNode(hasTestTag("script-copy"))
        rule.waitForIdle()

        assertEquals(listOf(HapticEffect.Success), played)
    }

    private fun show(dark: Boolean, reduceMotion: Boolean = false, screen: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MontageTheme(darkTheme = dark, reduceMotion = reduceMotion) { CompositionLocalProvider(LocalHaptics provides Haptics { played += it }) { screen() } }
        }
        rule.mainClock.advanceTimeBy(100)
    }

    private fun change(block: () -> Unit) {
        block()
        Snapshot.sendApplyNotifications()
    }
}

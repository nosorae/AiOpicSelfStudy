package io.github.nosorae.aiopic.ui.featurereport

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 회차 리포트와 답변 리포트의 상태마다 한 장. 등급 범위와 모델 섞임, 시간 부족, 평가 불가, 발음 평가 안 함은
 * 다크와 글자 200% 도 찍는다. 입력은 미리보기와 같은 [ReportSamples] 다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ReportScreenshotTest {
    @Config(qualifiers = "w360dp-h1100dp-xhdpi")
    @Test
    fun session_gradeRange_mixedModels() = session(ReportSamples.mixedModels)

    @Test fun session_gradeRange_mixedModels_dark() = session(ReportSamples.mixedModels, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun session_gradeRange_mixedModels_fontScale200() = session(ReportSamples.mixedModels)

    @Config(qualifiers = "w360dp-h1000dp-xhdpi")
    @Test
    fun session_timeShort() = session(ReportSamples.timeUp)

    @Test fun answer_evaluated() = answer(ReportSamples.answerEvaluated)

    @Config(qualifiers = "w360dp-h1100dp-xhdpi")
    @Test
    fun answer_pronunciationNotEvaluated() = answer(ReportSamples.pronunciationNotEvaluated)

    @Config(qualifiers = "w360dp-h1100dp-xhdpi")
    @Test
    fun answer_pronunciationNotEvaluated_dark() = answer(ReportSamples.pronunciationNotEvaluated, dark = true)

    @Test fun answer_unscorable() = answer(ReportSamples.answerUnscorable)

    @Config(fontScale = 2f)
    @Test
    fun answer_unscorable_fontScale200() = answer(ReportSamples.answerUnscorable)

    @Test fun answer_pending() = answer(ReportSamples.answerPending)

    private fun session(state: SessionReportUiState, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { SessionReportScreen(state = state, onOpenAnswer = {}) }
    }

    private fun answer(state: AnswerReportUiState, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { AnswerReportScreen(state = state) }
    }
}

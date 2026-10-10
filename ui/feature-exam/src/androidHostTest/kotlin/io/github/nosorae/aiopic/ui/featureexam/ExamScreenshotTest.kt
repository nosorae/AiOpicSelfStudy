package io.github.nosorae.aiopic.ui.featureexam

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 화면마다 라이트 한 장, 문항 화면은 진행마다 한 장. 녹음 중 화면과 서베이는 다크와 글자 200% 도 찍는다. 입력은 [ExamSamples] 다.
 * 서베이 라이트는 Part 1~4 가 한 장에 다 들어오게 높이를 넉넉히 잡는다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ExamScreenshotTest {
    @Config(qualifiers = "w360dp-h1800dp-xhdpi")
    @Test
    fun survey() = shot(ExamSamples.survey)

    @Test fun surveyReady() = shot(ExamSamples.surveyReady)

    @Test fun level() = shot(ExamSamples.level)

    @Test fun check() = shot(ExamSamples.check)

    @Test fun micError() = shot(ExamSamples.micError)

    @Test fun questionReady() = shot(ExamSamples.questionReady)

    @Test fun questionSpeaking() = shot(ExamSamples.questionSpeaking)

    @Test fun questionReplayable() = shot(ExamSamples.questionReplayable)

    @Test fun questionNextable() = shot(ExamSamples.questionNextable)

    @Test fun questionSaving() = shot(ExamSamples.questionSaving)

    @Test fun surveySaving() = shot(ExamSamples.surveySaving)

    /** 설정에서 답변 타이머를 켰을 때(F2.7). 꺼져 있으면 "녹음 중"만 보인다. */
    @Test fun questionTimer() = shot(ExamSamples.questionTimer)

    @Test fun recalibrate() = shot(ExamSamples.recalibrate)

    @Test fun survey_dark() = shot(ExamSamples.survey, dark = true)

    @Test fun questionReplayable_dark() = shot(ExamSamples.questionReplayable, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun survey_fontScale200() = shot(ExamSamples.survey)

    @Config(fontScale = 2f)
    @Test
    fun questionReplayable_fontScale200() = shot(ExamSamples.questionReplayable)

    private fun shot(state: ExamUiState, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { ExamScreen(state = state, onIntent = {}) }
    }
}

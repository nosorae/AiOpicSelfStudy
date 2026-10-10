package io.github.nosorae.aiopic.ui.featureexam

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 화면마다 라이트 한 장, 문항 화면은 진행마다 한 장. 서베이, 수준 선택, 재조정은 다크와 글자 200% 도 찍는다.
 * 녹음 중 화면은 목소리 띠가 계속 움직여 [ExamRecordingScreenshotTest] 가 찍는다. 입력은 [ExamSamples] 다.
 * 서베이는 한 화면에 질문 하나라 질문 화면, 묶음 화면, 마지막 묶음(모자람, 채움)을 한 장씩 찍는다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ExamScreenshotTest {
    @Test fun survey() = shot(ExamSamples.survey)

    @Test fun surveyGroup() = shot(ExamSamples.surveyGroup)

    @Test fun surveyShort() = shot(ExamSamples.surveyShort)

    @Test fun surveyReady() = shot(ExamSamples.surveyReady)

    @Test fun level() = shot(ExamSamples.level)

    @Test fun check() = shot(ExamSamples.check)

    @Test fun micError() = shot(ExamSamples.micError)

    @Test fun recordingError() = shot(ExamSamples.recordingError)

    @Test fun questionReady() = shot(ExamSamples.questionReady)

    @Test fun questionSpeaking() = shot(ExamSamples.questionSpeaking)

    @Test fun surveySaving() = shot(ExamSamples.surveySaving)

    @Test fun recalibrate() = shot(ExamSamples.recalibrate)

    @Test fun survey_dark() = shot(ExamSamples.survey, dark = true)

    @Test fun surveyGroup_dark() = shot(ExamSamples.surveyGroup, dark = true)

    @Test fun surveyReady_dark() = shot(ExamSamples.surveyReady, dark = true)

    @Test fun level_dark() = shot(ExamSamples.level, dark = true)

    @Test fun recalibrate_dark() = shot(ExamSamples.recalibrate, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun survey_fontScale200() = shot(ExamSamples.survey)

    @Config(fontScale = 2f)
    @Test
    fun surveyGroup_fontScale200() = shot(ExamSamples.surveyGroup)

    @Config(fontScale = 2f)
    @Test
    fun surveyReady_fontScale200() = shot(ExamSamples.surveyReady)

    @Config(fontScale = 2f)
    @Test
    fun level_fontScale200() = shot(ExamSamples.level)

    @Config(fontScale = 2f)
    @Test
    fun recalibrate_fontScale200() = shot(ExamSamples.recalibrate)

    private fun shot(state: ExamUiState, dark: Boolean = false) = captureRoboImage {
        MontageTheme(darkTheme = dark) { ExamScreen(state = state, onIntent = {}) }
    }
}

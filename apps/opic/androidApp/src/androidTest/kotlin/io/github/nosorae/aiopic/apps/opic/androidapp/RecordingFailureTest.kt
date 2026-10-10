package io.github.nosorae.aiopic.apps.opic.androidapp

import android.Manifest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 녹음 조각을 쓰지 못했을 때 한 문항 연습 화면이 이유와 다시 녹음 버튼을 보이는지 실제 앱으로 본다(#152).
 * 계측 인자 `recordFail true` 를 줄 때만 돈다. 이 테스트가 첫 실행을 지나가면 [ExamE2eTest] 가 기대하는 첫 실행 화면이 사라져서
 * 평소 묶음에서는 건너뛰고, 앱 데이터를 지운 뒤 따로 돌린다.
 */
@RunWith(AndroidJUnit4::class)
class RecordingFailureTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun practiceShowsReasonAndRetryWhenChunkWriteFails() {
        assumeTrue("recordFail 계측 인자가 없다", recordFailRequested())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, Manifest.permission.RECORD_AUDIO)
        ActivityScenario.launch(MainActivity::class.java)

        click("Gemini")
        compose.onNode(hasSetTextAction()).performTextInput("test-key")
        click("키 확인하고 저장")
        click("동의하고 계속하기")

        click("한 문항 풀기")
        click("Play")
        waitFor(hasText("Next") and isEnabled(), QUESTION_TIMEOUT)
        compose.onNode(hasText("Next") and isEnabled()).performClick()

        waitFor(hasText(MESSAGE), SCREEN_TIMEOUT)
        click("다시 녹음")
        waitFor(hasText("녹음 중", substring = true), QUESTION_TIMEOUT)
    }

    private fun click(text: String) {
        waitFor(hasText(text) and isEnabled(), SCREEN_TIMEOUT)
        compose.onAllNodes(hasText(text) and isEnabled()).onFirst().performClick()
    }

    private fun waitFor(matcher: SemanticsMatcher, timeoutMs: Long) = compose.waitUntil(timeoutMs) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private companion object {
        const val MESSAGE = "녹음을 저장하지 못했습니다. 기기 저장 공간을 확인하고 다시 녹음을 누르세요."
        const val SCREEN_TIMEOUT = 15_000L
        const val QUESTION_TIMEOUT = 60_000L
    }
}

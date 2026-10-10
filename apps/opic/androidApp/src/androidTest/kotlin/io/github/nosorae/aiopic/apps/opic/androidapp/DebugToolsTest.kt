package io.github.nosorae.aiopic.apps.opic.androidapp

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 디버그 빌드의 설정 화면 "개발자 도구" 줄로 디버깅 화면을 열고, 키 확인 요청이 Network 탭에 남았는지 본다.
 * 계측 인자 `debugTools true` 를 줄 때만 돈다. 첫 실행을 지나가면 [ExamE2eTest] 가 기대하는 첫 실행 화면이 사라져서 평소 묶음에서는 건너뛴다.
 */
@RunWith(AndroidJUnit4::class)
class DebugToolsTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun settingsRowOpensDebugToolsWithKeyCheckCall() {
        assumeTrue("debugTools 계측 인자가 없다", InstrumentationRegistry.getArguments().getString("debugTools") == "true")
        ActivityScenario.launch(MainActivity::class.java)

        // 첫 실행이면 키 입력 화면, 아니면 탭이 있는 홈이 뜬다.
        waitFor(hasSetTextAction() or hasText("설정"))
        if (compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()) {
            click("Gemini")
            compose.onNode(hasSetTextAction()).performTextInput("test-key")
            click("키 확인하고 저장")
            click("동의하고 계속하기")
        }
        click("설정")
        waitFor(hasTestTag("settings-developer-tools"))
        compose.onNode(hasTestTag("settings-developer-tools")).performScrollTo().performClick()

        click("Network")
        waitFor(hasText("POST 200", substring = true))
        waitFor(hasText("generateContent", substring = true))
    }

    private fun click(text: String) {
        waitFor(hasText(text) and isEnabled())
        compose.onAllNodes(hasText(text) and isEnabled()).onFirst().performClick()
    }

    private fun waitFor(matcher: SemanticsMatcher) = compose.waitUntil(TIMEOUT) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private companion object {
        const val TIMEOUT = 15_000L
    }
}

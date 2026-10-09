package io.github.nosorae.aiopic.apps.opic.shared

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 시작 분기(S0, F1.0). 저장된 키와 동의만 보고 첫 화면을 정하고, 정하기 전에는 아무것도 그리지 않은 채 스플래시를 잡아 둔다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class StartDestinationTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val settings = AppSettings.inMemory()

    private fun start(keyVault: KeyVault): AppGraph {
        val graph = testAppGraph(settings, keyVault)
        rule.setContent { OpicApp(graph, graph.startViewModel) }
        return graph
    }

    private fun savedKey() = FakeKeyVault().apply { runBlocking { put(KeyProvider.Gemini.id, "test-key") } }

    @Test
    fun `F1_0 키가 없으면 탭 없는 키 입력 화면에서 시작한다`() {
        start(FakeKeyVault())

        rule.onNodeWithText("키 확인하고 저장").assertIsDisplayed()
        rule.onNodeWithText("기록").assertDoesNotExist()
    }

    @Test
    fun `F1_0 키는 있고 동의 전이면 동의 화면에서 시작한다`() {
        start(savedKey())

        rule.onNodeWithText("동의하고 계속하기").assertIsDisplayed()
        rule.onNodeWithText("기록").assertDoesNotExist()
    }

    @Test
    fun `F1_0 키와 동의가 모두 있으면 홈에서 시작한다`() {
        runBlocking { settings.give() }

        start(savedKey())

        rule.onNodeWithText("모의시험 시작").assertIsDisplayed()
        rule.onNodeWithText("기록").assertIsDisplayed()
    }

    @Test
    fun `F1_0 동의 화면에서 동의하면 홈으로 간다`() {
        start(savedKey())

        rule.onNodeWithText("동의하고 계속하기").performClick()

        rule.waitUntil(5_000) { rule.onAllNodes(hasText("모의시험 시작")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun `F1_0 저장된 상태를 다 읽기 전에는 스플래시를 잡아 두고 화면을 그리지 않는다`() {
        val opened = CompletableDeferred<Unit>()
        val slowVault = object : KeyVault by FakeKeyVault() {
            override suspend fun get(provider: String): String? {
                opened.await()
                return null
            }
        }
        val graph = testAppGraph(settings, slowVault)
        val startViewModel = graph.startViewModel
        rule.setContent { OpicApp(graph, startViewModel) }

        assertTrue(startViewModel.uiState.value.keepSplash)
        rule.onNodeWithText("키 확인하고 저장").assertDoesNotExist()
        rule.onNodeWithText("모의시험 시작").assertDoesNotExist()

        opened.complete(Unit)
        rule.waitForIdle()

        assertFalse(startViewModel.uiState.value.keepSplash)
        rule.onNodeWithText("키 확인하고 저장").assertIsDisplayed()
    }
}

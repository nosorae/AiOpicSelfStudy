package io.github.nosorae.aiopic.ui.featureonboarding.key

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 키 받기 버튼이 제공사의 키 발급 페이지를 열고, 입력칸의 지우기 버튼이 키를 비우는지 본다(F1.1). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class KeyEntryScreenTest {
    @get:Rule val rule = createComposeRule()

    private val opened = mutableListOf<String>()
    private val intents = mutableListOf<KeyEntryIntent>()
    private val uriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            opened += uri
        }
    }

    private fun show(state: KeyEntryUiState) = rule.setContent {
        CompositionLocalProvider(LocalUriHandler provides uriHandler) {
            MontageTheme { KeyEntryScreen(state = state, onIntent = { intents += it }, onOpenFreeTierNotice = {}) }
        }
    }

    @Test
    fun `키 받기를 누르면 고른 제공사의 키 발급 페이지를 연다`() {
        show(KeyEntrySamples.ready)

        rule.onNodeWithText("Gemini 키 받기").performScrollTo().performClick()

        assertEquals(listOf("https://aistudio.google.com/apikey"), opened)
    }

    @Test
    fun `제공사를 고르기 전에도 첫 제공사의 키 발급 페이지를 연다`() {
        show(KeyEntrySamples.empty)

        rule.onNodeWithText("Gemini 키 받기").performScrollTo().performClick()

        assertEquals(listOf(KeyProvider.Gemini.keyPageUrl), opened)
    }

    @Test
    fun `키가 가려지지 않고 그대로 보인다`() {
        show(KeyEntrySamples.ready)

        rule.onNodeWithText("test-key").assertExists()
    }

    @Test
    fun `지우기를 누르면 키를 비운다`() {
        show(KeyEntrySamples.ready)

        rule.onNodeWithContentDescription("키 지우기").performScrollTo().performClick()

        assertEquals(listOf<KeyEntryIntent>(KeyEntryIntent.ChangeKey("")), intents)
    }

    @Test
    fun `키가 비어 있으면 지우기 버튼이 없다`() {
        show(KeyEntrySamples.empty)

        rule.onNodeWithContentDescription("키 지우기").assertDoesNotExist()
    }

    @Test
    fun `키를 확인하는 동안 저장 버튼은 진행 표시를 보이고 눌리지 않는다`() {
        show(KeyEntrySamples.checking)

        rule.onNodeWithContentDescription("처리 중").assertIsNotEnabled()
        rule.onNodeWithText("키 확인하고 저장").assertDoesNotExist()
        rule.onNodeWithContentDescription("처리 중").performClick()

        assertEquals(emptyList<KeyEntryIntent>(), intents)
    }

    @Test
    fun `키를 확인하는 동안 키 받기와 무료 등급 안내도 눌리지 않는다`() {
        show(KeyEntrySamples.checking)

        rule.onNodeWithText("Gemini 키 받기").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("무료 등급 안내 보기").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `키를 확인하는 동안 무엇을 기다리는지 버튼 위에 보인다`() {
        show(KeyEntrySamples.checking)

        rule.onNodeWithText("Gemini 에 키가 맞는지 묻고 있습니다. 맞으면 저장하고 바로 넘어갑니다.").assertExists()
    }

    @Test
    fun `키가 확인되면 저장 버튼 대신 확인 표시가 보이고 입력은 막힌다`() {
        show(KeyEntrySamples.saved)

        rule.onNodeWithText("키를 확인했습니다").assertExists()
        rule.onNodeWithText("키 확인하고 저장").assertDoesNotExist()
        rule.onNodeWithText("Gemini 키 받기").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `동작 줄이기면 확인 체크가 첫 프레임부터 다 그려져 있다`() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MontageTheme(reduceMotion = true) { KeyEntryScreen(state = KeyEntrySamples.saved, onIntent = {}, onOpenFreeTierNotice = {}) }
        }
        val first = rule.onRoot().captureToImage().asAndroidBitmap()

        rule.mainClock.advanceTimeBy(2_000)

        assertTrue(first.sameAs(rule.onRoot().captureToImage().asAndroidBitmap()))
    }
}

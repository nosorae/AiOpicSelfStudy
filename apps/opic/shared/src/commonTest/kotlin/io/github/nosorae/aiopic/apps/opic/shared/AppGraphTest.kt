package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.apps.opic.shared.data.FakeWorkQueue
import io.github.nosorae.aiopic.apps.opic.shared.data.inMemoryDatabase
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.libs.llmgemini.api.LlmResult
import io.github.nosorae.aiopic.libs.recorder.api.Recorder
import io.github.nosorae.aiopic.libs.recorder.api.RecordingFailure
import io.github.nosorae.aiopic.libs.recorder.api.StoppedRecording
import io.github.nosorae.aiopic.libs.tts.api.Tts
import io.github.nosorae.aiopic.ui.featureonboarding.DomainError
import io.github.nosorae.aiopic.ui.featureonboarding.DomainResult
import io.github.nosorae.aiopic.ui.featureonboarding.consent.ConsentIntent
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider
import io.github.nosorae.aiopic.ui.featureonboarding.start.StartDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

/** 기기 대신 메모리에 두는 플랫폼 인자로 그래프를 만든다. 앱 틀 테스트도 이것을 쓴다. */
fun testAppGraph(settings: AppSettings = AppSettings.inMemory(), keyVault: KeyVault = FakeKeyVault()): AppGraph = createAppGraph(
    settings = settings,
    keyVault = keyVault,
    database = inMemoryDatabase(),
    recordingsRoot = "recordings",
    recorder = FakeRecorder,
    tts = FakeTts,
    workQueue = FakeWorkQueue(),
)

class FakeKeyVault : KeyVault {
    private val keys = mutableMapOf<String, String>()

    override suspend fun get(provider: String): String? = keys[provider]

    override suspend fun put(provider: String, key: String) {
        keys[provider] = key
    }

    override suspend fun delete(provider: String) {
        keys.remove(provider)
    }
}

object FakeRecorder : Recorder {
    override suspend fun start(directory: String): RecordingFailure? = null

    override suspend fun stop() = StoppedRecording(emptyList())

    override suspend fun chunks(directory: String) = emptyList<String>()

    override suspend fun merge(chunks: List<String>, output: String) = emptyList<Duration>()
}

object FakeTts : Tts {
    override suspend fun speak(text: String) = true

    override fun stop() = Unit
}

class AppGraphTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `그래프 하나로 모든 화면의 ViewModel 과 분석 처리기를 만든다`() {
        val graph = testAppGraph()

        graph.homeViewModel
        graph.settingsViewModel
        graph.consentViewModel
        graph.startViewModel
        graph.historyViewModel
        graph.flagExportViewModel
        graph.keyEntryViewModelFactory.create(KeyProvider.Gemini)
        graph.examViewModelFactory.create("session")
        graph.practiceViewModelFactory.create("answer")
        graph.sessionReportViewModelFactory.create("session")
        graph.answerReportViewModelFactory.create("answer")
        graph.analysisHandler
    }

    @Test
    fun `F1_3 동의는 그래프를 다시 만들어도 남는다`() = runTest(dispatcher) {
        val disk = mutableMapOf<String, String>()
        val keyVault = FakeKeyVault().apply { put(KeyProvider.Gemini.id, "test-key") }
        fun reopen() = testAppGraph(AppSettings(read = { disk[it] }, write = { key, value -> disk[key] = value }), keyVault)

        val before = reopen().startViewModel
        advanceUntilIdle()
        assertEquals(StartDestination.Consent, before.uiState.value.destination)

        reopen().consentViewModel.onIntent(ConsentIntent.GiveConsent)
        advanceUntilIdle()

        val after = reopen().startViewModel
        advanceUntilIdle()
        assertEquals(StartDestination.Home, after.uiState.value.destination)
    }

    @Test
    fun `F1_1 Gemini 가 키를 거절하면 틀린 키이고 연결이나 한도 문제면 잠시 못 씀이다`() {
        assertEquals(DomainResult.Success(Unit), geminiKeyCheck(LlmResult.Success("ok")))
        assertEquals(DomainResult.Failure(DomainError.InvalidKey), geminiKeyCheck(LlmResult.InvalidKey))
        assertEquals(DomainResult.Failure(DomainError.Unavailable), geminiKeyCheck(LlmResult.Retryable(429)))
        assertEquals(DomainResult.Failure(DomainError.Unavailable), geminiKeyCheck(LlmResult.Retryable(null)))
    }
}

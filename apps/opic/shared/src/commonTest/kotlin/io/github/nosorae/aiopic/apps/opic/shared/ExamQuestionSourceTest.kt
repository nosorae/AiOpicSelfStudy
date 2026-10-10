package io.github.nosorae.aiopic.apps.opic.shared

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.nosorae.aiopic.apps.opic.shared.data.FakeWorkQueue
import io.github.nosorae.aiopic.apps.opic.shared.data.inMemoryDatabase
import io.github.nosorae.aiopic.engine.aigemini.GeminiModels
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.github.nosorae.aiopic.libs.tts.api.Tts
import io.github.nosorae.aiopic.packs.opicen.OpicEn
import io.github.nosorae.aiopic.packs.opicen.OpicQuestionType
import io.github.nosorae.aiopic.packs.opicen.PromptFile
import io.github.nosorae.aiopic.ui.featureexam.ExamIntent
import io.github.nosorae.aiopic.ui.featureexam.ExamViewModel
import io.github.nosorae.aiopic.ui.featureexam.QuestionPhase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 모의시험 출제(F6.1, F6.2)가 앱 그래프에서 이어지는지 본다. 그래프를 메모리 DB 와 가짜 Gemini 로 만들고 시험 화면의 ViewModel 에
 * 서베이, 수준, 점검을 보낸 뒤 기기 음성이 읽은 질문을 본다. 호스트 JVM 테스트는 시험 팩 파일을 못 읽어서 문항 풀과 출제 프롬프트만 바꿔 넣는다.
 */
class ExamQuestionSourceTest {
    private val dispatcher = StandardTestDispatcher()
    private val db = inMemoryDatabase()

    // 앱을 다시 켜도 남는 것: 설정, 키, DB
    private val prefs = mutableMapOf<String, String>()
    private val keyVault = FakeKeyVault()
    private val viewModels = mutableListOf<ViewModelStore>()
    private val spoken = mutableListOf<String>()
    private val prompts = mutableListOf<String>()
    private var now = Duration.ZERO

    private val topics = OpicEn.surveyTopics.take(12)

    /** 수준 1 의 문항 유형 순서. 맨 앞이 점검의 연습 문항이다. */
    private val order = OpicEn.examSpec.steps.filterIsInstance<ExamStep.Ask>().filter { it.minLevel <= 1 }.map { it.questionType }.let { listOf(it.first()) + it }

    private val pool = OpicQuestionType.entries.flatMap { type ->
        (1..3).map { Question("pool-${type.name}-$it", "pool ${type.type.id} $it", type.type, if (type == OpicQuestionType.SelfIntro) "self" else topics[it]) }
    }

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    /** 시험 화면의 1초 시계가 돌고 있으면 runTest 가 끝나지 않아서, 끝날 때 ViewModel 을 모두 치운다. */
    private fun exam(body: suspend () -> Unit) = runTest(dispatcher) {
        try {
            body()
        } finally {
            viewModels.forEach(ViewModelStore::clear)
        }
    }

    /** 출제 요청에 [order] 그대로 "generated 번호" 질문을 돌려주는 응답. */
    private val generated = buildJsonObject {
        val text = buildJsonObject {
            putJsonArray("questions") {
                order.forEachIndexed { number, type ->
                    addJsonObject {
                        put("number", number)
                        put("type", type.id)
                        put("topic", if (type == OpicQuestionType.SelfIntro.type) "self" else topics[number / 3])
                        put("text", "generated $number")
                    }
                }
            }
        }.toString()
        putJsonArray("candidates") { addJsonObject { putJsonObject("content") { putJsonArray("parts") { addJsonObject { put("text", text) } } } } }
    }.toString()

    /** 앱을 켜고 회차 s1 의 시험 화면을 연다. [gemini] 는 출제 요청에 돌려줄 상태다. */
    private suspend fun launch(gemini: HttpStatusCode): ExamViewModel {
        val settings = AppSettings(read = { prefs[it] }, write = { key, value -> prefs[key] = value }).apply { give() }
        keyVault.put(GeminiModels.PROVIDER, "test-key")
        val graph = createAppGraph(
            settings = settings,
            keyVault = keyVault,
            database = db,
            recordingsRoot = "recordings",
            recorder = FakeRecorder,
            tts = SpeechLog(spoken),
            workQueue = FakeWorkQueue(),
            gemini = GeminiClient(
                HttpClient(
                    MockEngine {
                        prompts += it.body.toByteArray().decodeToString()
                        respond(if (gemini == HttpStatusCode.OK) generated else "{}", gemini)
                    },
                ),
            ),
            timer = { now },
            pack = ExamPack(pool = { pool }, questionsPrompt = { PromptFile("questions-test", "{{topics}} | {{level}} | {{recalibration}} | {{weakTypes}} | {{order}}") }),
        )
        val store = ViewModelStore().also(viewModels::add)
        return ViewModelProvider.create(store, viewModelFactory { initializer { graph.examViewModelFactory.create("s1") } })[ExamViewModel::class]
    }

    /** 서베이, 수준 1, 점검 연습 문항을 지나 1번 문항의 질문을 듣는다. */
    private suspend fun ExamViewModel.firstQuestion(): String {
        uiState.first { it.step is ExamStep.Survey }
        topics.forEach { onIntent(ExamIntent.ToggleTopic(it)) }
        onIntent(ExamIntent.SubmitSurvey)
        uiState.first { it.step == ExamStep.LevelSelect }
        onIntent(ExamIntent.SelectLevel(1))
        onIntent(ExamIntent.SubmitLevel)
        uiState.first { it.step == ExamStep.Check }
        answer()
        uiState.first { it.questionNumber == 1 && it.phase == QuestionPhase.Ready }
        onIntent(ExamIntent.PlayQuestion)
        uiState.first { it.phase == QuestionPhase.Recording }
        return spoken.last()
    }

    private suspend fun ExamViewModel.answer() {
        onIntent(ExamIntent.PlayQuestion)
        uiState.first { it.phase == QuestionPhase.Recording }
        now += 11.seconds
        onIntent(ExamIntent.SubmitAnswer)
        uiState.first { it.phase != QuestionPhase.Recording }
    }

    @Test
    fun `F6_1 시험을 시작하면 Gemini 가 만든 질문이 점검과 첫 문항에 나오고 출제 요청에 주제와 수준과 순서를 보낸다`() = exam {
        assertEquals("generated 1", launch(HttpStatusCode.OK).firstQuestion())

        assertEquals("generated 0", spoken.first())
        val request = prompts.single()
        assertTrue(topics.all { it in request } && "| 1 |" in request && "0. self-intro" in request && "12. roleplay-solve" in request, request)
    }

    @Test
    fun `F6_2 Gemini 가 실패하면 내장 풀 질문이 나온다`() = exam {
        val first = launch(HttpStatusCode.ServiceUnavailable).firstQuestion()

        assertTrue(first.startsWith("pool self-intro"), first)
    }

    @Test
    fun `F6_2 동의가 없으면 Gemini 에 보내지 않고 내장 풀 질문이 나온다`() = exam {
        val exam = launch(HttpStatusCode.OK)
        AppSettings(read = { prefs[it] }, write = { key, value -> prefs[key] = value }).revoke()

        assertTrue(exam.firstQuestion().startsWith("pool"))
        assertEquals(emptyList(), prompts)
    }

    @Test
    fun `F3_3 그래프를 다시 만들어 이어 보면 Gemini 가 실패해도 같은 질문이다`() = exam {
        val first = launch(HttpStatusCode.OK).firstQuestion()
        viewModels.forEach(ViewModelStore::clear)

        val resumed = launch(HttpStatusCode.ServiceUnavailable)
        resumed.uiState.first { it.step is ExamStep.Ask }
        resumed.onIntent(ExamIntent.PlayQuestion)
        resumed.uiState.first { it.phase == QuestionPhase.Recording }

        assertEquals(first to "generated 1", spoken.last() to first)
        assertEquals(1, prompts.size)
    }
}

/** 기기 음성 대신 읽은 글을 모은다. */
private class SpeechLog(private val spoken: MutableList<String>) : Tts {
    override suspend fun speak(text: String): Boolean = spoken.add(text)

    override fun stop() = Unit
}

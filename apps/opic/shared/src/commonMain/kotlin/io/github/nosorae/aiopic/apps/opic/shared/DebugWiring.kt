package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.libs.debug.DebugLevel
import io.github.nosorae.aiopic.libs.debug.DebugTools
import io.github.nosorae.aiopic.libs.debug.network.fakeHttpEngine
import io.github.nosorae.aiopic.libs.debug.network.installDebugTools
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.github.nosorae.aiopic.libs.recorder.api.Recorder
import io.github.nosorae.aiopic.libs.recorder.api.RecordingFailure
import io.github.nosorae.aiopic.libs.recorder.api.StoppedRecording
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 디버깅 화면의 스위치. 릴리스 빌드는 빈 모듈(debug-noop)이라 늘 꺼져 있어 가짜 Gemini 가 켜질 길이 없다.
 * 기기에 `debug_tools.<key>` 로 저장된다. XCUITest 는 실행 인자 `-debug_tools.<key> YES`, Android 계측은 저장소에 미리 써서 켠다.
 */
internal object DebugSwitches {
    val fakeGemini = DebugTools.switch("fake_gemini", "Fake Gemini (applies after restart)")
    val slowResponse = DebugTools.switch("slow_response", "Slow Gemini response (3 s)")
    val recordFail = DebugTools.switch("record_fail", "Recording chunk write fails")
}

/**
 * 요청과 응답을 디버깅 화면에 남기는 Gemini 클라이언트. [slow] 가 켜져 있으면 요청마다 [slowDelay] 기다린 뒤 보낸다.
 * [fakeEngine] 이 있으면 보내지 않고 그 엔진의 응답("OK")을 돌려준다. 가짜 키가 키 확인을 넘고(F1.1), 출제와 분석은 응답을 못 읽어 내장 문항과 분석 실패로 간다.
 * 가짜 엔진은 실제 디버깅 모듈만 주고 릴리스의 빈 모듈은 null 이라, 릴리스 빌드에는 가짜 응답 코드가 실리지 않는다.
 */
// ponytail: 가짜 여부는 클라이언트를 만들 때 한 번 읽는다. 스위치를 바꾸면 앱을 다시 시작해야 한다. 요청마다 바꿔야 하면 엔진을 감싼다.
fun debugGeminiClient(
    fakeEngine: HttpClientEngine? = if (DebugSwitches.fakeGemini.enabled) fakeHttpEngine(OK_RESPONSE) else null,
    slow: () -> Boolean = { DebugSwitches.slowResponse.enabled },
    slowDelay: Duration = 3.seconds,
): GeminiClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        installDebugTools()
        install(createClientPlugin("SlowResponse") { onRequest { _, _ -> if (slow()) delay(slowDelay) } })
    }
    return GeminiClient(if (fakeEngine != null) HttpClient(fakeEngine, config) else HttpClient(config))
}

internal const val OK_RESPONSE = """{"candidates":[{"content":{"parts":[{"text":"OK"}]}}]}"""

/** 녹음 시작과 끝을 디버깅 로그에 남긴다. [fail] 이 켜져 있으면 녹음은 그대로 하고 멈출 때 조각 쓰기에 실패했다고 알린다. */
fun Recorder.withDebugTools(fail: () -> Boolean = { DebugSwitches.recordFail.enabled }): Recorder {
    val real = this
    return object : Recorder by real {
        override suspend fun start(directory: String): RecordingFailure? = real.start(directory).also {
            DebugTools.log(if (it == null) DebugLevel.Info else DebugLevel.Warn, TAG_RECORDER, "start ${it ?: "ok"}")
        }

        override suspend fun stop(): StoppedRecording {
            val stopped = real.stop().let { if (fail()) it.copy(failure = RecordingFailure.WriteFailed) else it }
            DebugTools.log(if (stopped.failure == null) DebugLevel.Info else DebugLevel.Warn, TAG_RECORDER, "stop ${stopped.chunks.size} chunks ${stopped.failure ?: "ok"}")
            return stopped
        }
    }
}

private const val TAG_RECORDER = "Recorder"

/**
 * 디버깅 화면에 이 앱의 패널(대기열, 오디오 세션, 설정 값, DB)을 더한다. 대기열과 오디오는 플랫폼마다 읽는 법이 달라 앱 틀이 넘긴다.
 * 패널은 화면을 열 때 읽는다.
 */
fun AppGraph.registerDebugPanels(queue: suspend () -> List<String>, audio: suspend () -> List<Pair<String, String>>) {
    DebugTools.panel("Queue") { queue().let { ids -> listOf("pending" to ids.size.toString()) + ids.mapIndexed { i, id -> "#${i + 1}" to id } } }
    DebugTools.panel("Audio session", audio)
    DebugTools.panel("Settings") { settings.debugRows() }
    DebugTools.panel("DB") { dbRows(opicStore) }
}

/** 표마다 행 수와 마지막으로 저장한 행. 회차와 답변은 저장한 순서(rowid)로 읽는다. */
internal suspend fun dbRows(store: OpicStore): List<Pair<String, String>> {
    val sessions = store.sessions().first()
    val answers = store.answers().first()
    return listOf(
        "exam_session" to "${sessions.size} rows" + sessions.lastOrNull()?.let { ", last ${it.id} (step ${it.stepIndex})" }.orEmpty(),
        "answer" to "${answers.size} rows" + answers.lastOrNull()?.let { ", last ${it.id} (${it.analysis::class.simpleName})" }.orEmpty(),
        "correction_flag" to "${store.flags().first().size} rows",
    )
}

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
import io.ktor.http.content.TextContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 디버깅 화면의 스위치. 릴리스 빌드는 빈 모듈(debug-noop)이라 늘 꺼져 있어 가짜 Gemini 가 켜질 길이 없다.
 * 기기에 `debug_tools.<key>` 로 저장된다. XCUITest 는 실행 인자 `-debug_tools.<key> YES`, Android 계측은 저장소에 미리 써서 켠다.
 */
internal object DebugSwitches {
    val fakeGemini = DebugTools.switch("fake_gemini", "Fake Gemini (applies after restart)")
    val slowResponse = DebugTools.switch("slow_response", "Slow Gemini response (3 s)")
    val recordFail = DebugTools.switch("record_fail", "Recording chunk write fails")
    val dropOnce = DebugTools.switch("drop_once", "Fake Gemini: first analysis drops, then succeeds")
    val offline = DebugTools.switch("offline", "Fake Gemini: every analysis drops")
    val stall = DebugTools.switch("stall", "Every analysis request hangs past the time limit")
}

/**
 * 요청과 응답을 디버깅 화면에 남기는 Gemini 클라이언트. [slow] 가 켜져 있으면 요청마다 [slowDelay] 기다린 뒤 보낸다.
 * [stall] 이 켜져 있으면 녹음을 실은 요청(분석, 등급)이 분석 작업의 시간 제한보다 오래 멈춘다(#205).
 * [fakeEngine] 이 있으면 보내지 않고 그 엔진의 응답("OK")을 돌려준다. 가짜 키가 키 확인을 넘고(F1.1), 출제와 분석은 응답을 못 읽어 내장 문항과 분석 실패로 간다.
 * 가짜 엔진은 실제 디버깅 모듈만 주고 릴리스의 빈 모듈은 null 이라, 릴리스 빌드에는 가짜 응답 코드가 실리지 않는다.
 */
// ponytail: 가짜 여부는 클라이언트를 만들 때 한 번 읽는다. 스위치를 바꾸면 앱을 다시 시작해야 한다. 요청마다 바꿔야 하면 엔진을 감싼다.
fun debugGeminiClient(
    fakeEngine: HttpClientEngine? = if (DebugSwitches.fakeGemini.enabled) fakeHttpEngine(FakeGemini()::respond) else null,
    slow: () -> Boolean = { DebugSwitches.slowResponse.enabled },
    slowDelay: Duration = 3.seconds,
    stall: () -> Boolean = { DebugSwitches.stall.enabled },
): GeminiClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        installDebugTools()
        install(
            createClientPlugin("SlowResponse") {
                onRequest { _, content ->
                    if (slow()) delay(slowDelay)
                    if (stall() && "inlineData" in (content as? TextContent)?.text.orEmpty()) delay(STALL)
                }
            },
        )
    }
    return GeminiClient(if (fakeEngine != null) HttpClient(fakeEngine, config) else HttpClient(config))
}

/** 분석 작업의 시간 제한(2분)보다 길다. */
private val STALL = 10.minutes

internal const val OK_RESPONSE = """{"candidates":[{"content":{"parts":[{"text":"OK"}]}}]}"""

/** 출력 검사를 통과하는 답변 분석 결과. 연결이 돌아온 뒤 분석이 끝나는지 볼 때 돌려준다. */
internal const val ANALYSIS_RESPONSE =
    """{"candidates":[{"content":{"parts":[{"text":"{\"transcript\":\"I live in Seoul.\",\"corrections\":[],\"upgrades\":[],\"tenseConsistent\":true,\"sentenceCount\":1,\"wordsPerMinute\":120.0,\"pauses\":[],\"pronunciation\":\"Clear.\",\"score\":80}"}]}}]}"""

/**
 * 가짜 Gemini 의 응답(#203). 녹음을 실은 요청(답변 분석, 회차 등급)만 연결 끊김 스위치를 따르고, 키 확인과 출제는 늘 "OK" 다.
 * [dropOnce] 면 첫 분석 요청은 연결 오류, 그 뒤로는 읽을 수 있는 분석 결과다. 연결이 잠깐 끊겼다 돌아온 경우다.
 * [offline] 이면 분석 요청이 모두 연결 오류다. 연결이 돌아오지 않는 경우다.
 * 실제 기기 네트워크를 끊지 않고 같은 길(연결 오류, 실패 저장, 대기열 다시 보내기)을 탄다.
 */
// ponytail: 끊김 한 번은 프로세스마다 한 번이다. 대기열이 한 번에 하나만 보내서 잠금 없이 센다.
internal class FakeGemini(
    private val dropOnce: () -> Boolean = { DebugSwitches.dropOnce.enabled },
    private val offline: () -> Boolean = { DebugSwitches.offline.enabled },
) {
    private var dropped = false

    fun respond(requestBody: String): String {
        if ("inlineData" !in requestBody) return OK_RESPONSE
        if (offline()) throw IOException("debug: offline")
        if (!dropOnce()) return OK_RESPONSE
        if (!dropped) {
            dropped = true
            throw IOException("debug: connection dropped once")
        }
        return ANALYSIS_RESPONSE
    }
}

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
 * 디버깅 화면에 이 앱의 패널(분석 작업, 오디오 세션, 설정 값, DB)을 더한다. 오디오는 플랫폼마다 읽는 법이 달라 앱 틀이 넘긴다.
 * 패널은 화면을 열 때 읽는다.
 */
fun AppGraph.registerDebugPanels(audio: suspend () -> List<Pair<String, String>>) {
    DebugTools.panel("Queue") { opicStore.jobs().first().filter { it.state.active }.map { "${it.kind} ${it.targetId}" to "${it.state} (${it.attempts})" } }
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
        "analysis_job" to store.jobs().first().let { jobs -> "${jobs.size} rows" + jobs.groupingBy { it.state }.eachCount().entries.joinToString("") { ", ${it.key} ${it.value}" } },
    )
}

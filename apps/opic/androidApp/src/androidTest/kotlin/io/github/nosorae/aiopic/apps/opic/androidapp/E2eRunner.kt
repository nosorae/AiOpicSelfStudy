package io.github.nosorae.aiopic.apps.opic.androidapp

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import io.github.nosorae.aiopic.libs.debug.network.installDebugTools
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * 시나리오 테스트가 앱을 [E2eApplication] 으로 띄운다. Gemini 만 가짜이고 DB, 녹음기, 기기 음성, 대기열은 실제다.
 * 계측 인자 `recordFail true` 를 주면 디버깅 스위치 "녹음 실패"를 켜서 녹음기가 멈출 때마다 조각 쓰기에 실패했다고 알린다([RecordingFailureTest]).
 */
class E2eRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        trustNetwork()
    }

    /**
     * 답변 분석은 WorkManager 가 네트워크가 연결됐다고 볼 때만 돈다. 에뮬레이터는 인터넷이 되는데도 연결 확인 요청이 실패해
     * 네트워크를 "확인되지 않음"으로 두고, 그러면 분석이 "Constraints not met" 으로 계속 미뤄진다. Gemini 는 가짜라 실제 연결이 필요 없다.
     * 그래서 기기의 연결 확인을 끄고 Wi-Fi 를 한 번 껐다 켜서 네트워크를 다시 확인하게 한 뒤, 확인될 때까지 기다린다.
     * 비행기 모드는 쓰지 않는다. 이동통신이 끊겼다 붙으면 화면 구성이 바뀌어 테스트 중인 화면이 다시 만들어진다.
     */
    // ponytail: 기기 설정을 테스트 뒤에도 그대로 둔다. 테스트 전용 에뮬레이터라서다. 실기기에서 돌리면 settings delete global captive_portal_mode 로 되돌린다.
    private fun trustNetwork() {
        if (validated()) return
        shell("settings put global captive_portal_mode 0")
        shell("svc wifi disable")
        shell("svc wifi enable")
        val until = System.currentTimeMillis() + NETWORK_TIMEOUT_MS
        while (!validated() && System.currentTimeMillis() < until) Thread.sleep(500)
    }

    private fun validated(): Boolean {
        val connectivity = targetContext.getSystemService(ConnectivityManager::class.java)
        return connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }

    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command)).use { it.readBytes().decodeToString() }

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application = super.newApplication(cl, E2eApplication::class.java.name, context)
}

/**
 * 키 확인과 답변 분석 요청에 모두 같은 저장 응답을 돌려준다. 키 확인은 200 만 보고, 분석은 이 JSON 을 읽는다.
 * 출제 요청은 [QUESTION_DELAY] 늦게 답하고 응답을 못 읽어 내장 문항으로 간다. Play 가 출제를 기다리지 않는지 볼 때 쓴다(#150).
 * 요청과 응답은 디버깅 화면의 Network 탭에 남는다([DebugToolsTest]).
 */
class E2eApplication : OpicApplication() {
    override fun onCreate() {
        // 스위치는 libs/debug 가 SharedPreferences "debug_tools" 에 키 그대로 둔다. 그래프가 녹음기를 만들기 전에 쓴다.
        getSharedPreferences("debug_tools", MODE_PRIVATE).edit().putBoolean("record_fail", recordFailRequested()).commit()
        super.onCreate()
    }

    override fun gemini() = GeminiClient(
        HttpClient(
            MockEngine {
                if (QUESTION_PROMPT in it.body.toByteArray().decodeToString()) delay(QUESTION_DELAY)
                respond(ANSWER_RESPONSE, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ) { installDebugTools() },
    )
}

fun recordFailRequested() = InstrumentationRegistry.getArguments().getString("recordFail") == "true"

/** 시험 팩 출제 프롬프트의 첫 문장 일부. */
private const val QUESTION_PROMPT = "You write questions for an OPIc English speaking mock test"

private val QUESTION_DELAY = 3.seconds

private const val NETWORK_TIMEOUT_MS = 30_000L

/** 분석 화면에 보일 받아쓴 답변. 리포트에서 이 글을 찾는다. */
const val FAKE_TRANSCRIPT = "I live in Seoul and I go jogging every morning."

const val FAKE_SCORE = 80

private const val ANSWER_RESPONSE = """{"candidates":[{"content":{"role":"model","parts":[{"text":"{\"transcript\":\"$FAKE_TRANSCRIPT\",\"corrections\":[],\"upgrades\":[],\"tenseConsistent\":true,\"sentenceCount\":2,\"wordsPerMinute\":120.0,\"pauses\":[],\"pronunciation\":\"Clear.\",\"score\":$FAKE_SCORE}"}]},"finishReason":"STOP"}]}"""

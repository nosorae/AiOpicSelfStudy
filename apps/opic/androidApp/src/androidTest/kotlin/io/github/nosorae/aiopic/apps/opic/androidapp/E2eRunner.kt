package io.github.nosorae.aiopic.apps.opic.androidapp

import android.app.Application
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.github.nosorae.aiopic.libs.recorder.api.Recorder
import io.github.nosorae.aiopic.libs.recorder.api.RecordingFailure
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
 * 계측 인자 `recordFail true` 를 주면 녹음기가 멈출 때마다 조각 쓰기에 실패했다고 알린다([RecordingFailureTest]).
 */
class E2eRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application = super.newApplication(cl, E2eApplication::class.java.name, context)
}

/**
 * 키 확인과 답변 분석 요청에 모두 같은 저장 응답을 돌려준다. 키 확인은 200 만 보고, 분석은 이 JSON 을 읽는다.
 * 출제 요청은 [QUESTION_DELAY] 늦게 답하고 응답을 못 읽어 내장 문항으로 간다. Play 가 출제를 기다리지 않는지 볼 때 쓴다(#150).
 */
class E2eApplication : OpicApplication() {
    override fun recorder(): Recorder {
        val real = super.recorder()
        if (!recordFailRequested()) return real
        return object : Recorder by real {
            override suspend fun stop() = real.stop().copy(failure = RecordingFailure.WriteFailed)
        }
    }

    override fun gemini() = GeminiClient(
        HttpClient(
            MockEngine {
                if (QUESTION_PROMPT in it.body.toByteArray().decodeToString()) delay(QUESTION_DELAY)
                respond(ANSWER_RESPONSE, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
    )
}

fun recordFailRequested() = InstrumentationRegistry.getArguments().getString("recordFail") == "true"

/** 시험 팩 출제 프롬프트의 첫 문장 일부. */
private const val QUESTION_PROMPT = "You write questions for an OPIc English speaking mock test"

private val QUESTION_DELAY = 3.seconds

/** 분석 화면에 보일 받아쓴 답변. 리포트에서 이 글을 찾는다. */
const val FAKE_TRANSCRIPT = "I live in Seoul and I go jogging every morning."

const val FAKE_SCORE = 80

private const val ANSWER_RESPONSE = """{"candidates":[{"content":{"role":"model","parts":[{"text":"{\"transcript\":\"$FAKE_TRANSCRIPT\",\"corrections\":[],\"upgrades\":[],\"tenseConsistent\":true,\"sentenceCount\":2,\"wordsPerMinute\":120.0,\"pauses\":[],\"pronunciation\":\"Clear.\",\"score\":$FAKE_SCORE}"}]},"finishReason":"STOP"}]}"""

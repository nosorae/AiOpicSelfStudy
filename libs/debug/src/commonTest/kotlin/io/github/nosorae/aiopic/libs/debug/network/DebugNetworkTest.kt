package io.github.nosorae.aiopic.libs.debug.network

import io.github.nosorae.aiopic.libs.debug.DebugConfig
import io.github.nosorae.aiopic.libs.debug.DebugCore
import io.github.nosorae.aiopic.libs.debug.MemoryPlatform
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class DebugNetworkTest {
    private val clock = object : Clock {
        var now = Instant.parse("2026-10-10T01:00:00Z")
        override fun now() = now
    }
    private val key = "AIza" + "A".repeat(35)

    private fun core(config: DebugConfig = DebugConfig()) = DebugCore(clock).apply { install(config, MemoryPlatform()) }

    private fun client(core: DebugCore, engine: MockEngine) = HttpClient(engine) { installDebugTools(core) }

    @Test
    fun recordsTheCallWithKeysHiddenAndLeavesTheBodyForTheApp() = runTest {
        val core = core(DebugConfig(redactHeaders = setOf("X-App-Token")))
        val engine = MockEngine {
            clock.now += 1_500.milliseconds
            respond("""{"ok":"$key"}""", HttpStatusCode.Created, headersOf("Set-Cookie", "sid=1"))
        }

        val body = client(core, engine).post("https://x.dev/v1/models:go?alt=json&key=test-key") {
            header("x-goog-api-key", "test-key")
            header("Authorization", "Bearer test-token")
            header("X-App-Token", "app-secret")
            header("X-Trace", "t-1")
            setBody(TextContent("""{"prompt":"hi $key"}""", ContentType.Application.Json))
        }.bodyAsText()

        assertEquals("""{"ok":"$key"}""", body)
        val call = core.calls.value.single()
        assertEquals("POST", call.method)
        assertEquals("https://x.dev/v1/models:go?alt=json&key=***", call.url)
        assertEquals(201, call.status)
        assertEquals(1_500.milliseconds, call.duration)
        val sent = call.requestHeaders.toMap()
        assertEquals("***", sent["x-goog-api-key"])
        assertEquals("***", sent["Authorization"])
        assertEquals("***", sent["X-App-Token"])
        assertEquals("t-1", sent["X-Trace"])
        assertEquals("***", call.responseHeaders.toMap()["Set-Cookie"])
        assertEquals("""{"prompt":"hi ***"}""", call.requestBody)
        assertEquals("""{"ok":"***"}""", call.responseBody)
    }

    @Test
    fun cutsALongBodyAndWritesItsOriginalSizeWithoutLeavingHalfAKey() = runTest {
        val core = core(DebugConfig(maxBodyBytes = 100))
        val long = "a".repeat(90) + key + "b".repeat(500)
        val engine = MockEngine { respond(long) }

        client(core, engine).get("https://x.dev/long")

        val call = core.calls.value.single()
        assertEquals("a".repeat(90) + "***" + "b".repeat(7) + "\n(잘림: 원래 629 바이트)", call.responseBody)
        // 목록의 크기는 자르기 전 크기다.
        assertEquals(629, call.responseSize)
    }

    @Test
    fun keepsOnlyTheNewestCallsUpToTheLimit() = runTest {
        val core = core(DebugConfig(maxCalls = 2))
        val http = client(core, MockEngine { respond("") })

        (1..3).forEach { http.get("https://x.dev/$it") }

        assertEquals(listOf("https://x.dev/2", "https://x.dev/3"), core.calls.value.map { it.url })
    }

    @Test
    fun recordsAFailedCallAndPassesTheErrorOn() = runTest {
        val core = core()
        val http = client(core, MockEngine { throw IOException("no network $key") })

        assertFailsWith<IOException> { http.get("https://x.dev/fail") }

        val call = core.calls.value.single()
        assertEquals(null, call.status)
        assertContains(call.error.orEmpty(), "no network ***")
        assertFalse(key in call.error.orEmpty())
    }

    @Test
    fun reportHasTheNetworkSection() = runTest {
        val core = core()
        client(core, MockEngine { respond("pong") }).get("https://x.dev/ping")

        val report = core.report()

        assertContains(report, "# Network\n## 2026-10-10T01:00:00Z GET https://x.dev/ping 200 0s\n")
        assertContains(report, "< pong\n")
    }

    @Test
    fun fakeEngineAnswersWithoutSending() = runTest {
        val response = HttpClient(fakeHttpEngine("""{"a":1}""")!!).get("https://x.dev/ping")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"a":1}""", response.bodyAsText())
        assertEquals("application/json", response.headers["Content-Type"])
    }
}

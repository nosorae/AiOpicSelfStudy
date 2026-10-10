package io.github.nosorae.aiopic.libs.debug

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DebugCoreTest {
    private val clock = object : Clock {
        var now = Instant.parse("2026-10-10T01:00:00Z")
        override fun now() = now
    }
    private val platform = MemoryPlatform(info = listOf("app" to "1.0 (7)"))
    private val core = DebugCore(clock).apply { install(DebugConfig(maxLogs = 3), platform) }
    private val key = "AIza" + "A".repeat(35)
    private val geminiKey = "AQ." + "Ab_c-1".repeat(6)

    @Test
    fun keepsOnlyTheNewestLogsUpToTheLimit() {
        (1..5).forEach { core.log(DebugLevel.Info, "t", "m$it") }

        assertEquals(listOf("m3", "m4", "m5"), core.logs.value.map { it.message })
    }

    @Test
    fun redactsKeysInMessagesAndErrorsBeforeStoring() {
        val custom = DebugCore(clock).apply { install(DebugConfig(redactPatterns = listOf(Regex("secret-\\d+"))), platform) }

        custom.log(
            DebugLevel.Error,
            "t",
            "key=$key gemini=$geminiKey x-goog-api-key: test-key url=https://x.dev/v1?alt=json&key=test-key secret-42",
            IllegalStateException("bad $key $geminiKey"),
        )

        val log = custom.logs.value.single()
        assertEquals("key=*** gemini=*** x-goog-api-key: *** url=https://x.dev/v1?alt=json&key=*** ***", log.message)
        assertFalse(key in log.error.orEmpty())
        assertFalse(geminiKey in log.error.orEmpty())
        assertContains(log.error.orEmpty(), "IllegalStateException: bad *** ***")
    }

    @Test
    fun switchReadsTheStoredValueEveryTimeAndFallsBackToDefault() {
        val slow = core.switch("slow", "느린 응답", default = false)
        val fake = core.switch("fake", "가짜 엔진", default = true)

        assertFalse(slow.enabled)
        assertTrue(fake.enabled)
        core.setSwitch("slow", true)
        assertTrue(slow.enabled)
        assertEquals(true, platform.readSwitch("slow"))
        assertSame(slow, core.switch("slow", "느린 응답"))
    }

    @Test
    fun reportHasInfoSwitchesPanelsAndLogs() = runTest {
        core.switch("slow", "느린 응답")
        core.panel("대기열") { listOf("남은 작업" to "2") }
        core.panel("고장") { error("읽기 실패") }
        core.log(DebugLevel.Warn, "Key", "확인 실패")

        val report = core.report()

        assertContains(report, "app: 1.0 (7)")
        assertContains(report, "느린 응답 (slow): off")
        assertContains(report, "## 대기열\n남은 작업: 2")
        assertContains(report, "## 고장\n읽기 실패")
        assertContains(report, "2026-10-10T01:00:00Z W Key: 확인 실패")
    }

    @Test
    fun crashWritesTheMomentToAFileAndKeepsTheNewestFive() = runTest {
        core.log(DebugLevel.Info, "Exam", "문항 3")
        repeat(7) {
            clock.now += 1.seconds
            core.recordCrash(IllegalStateException("crash $it $key"))
        }

        val names = platform.crashNames().sorted()
        assertEquals(5, names.size)
        val newest = platform.readCrash(names.last())
        assertContains(newest, "IllegalStateException: crash 6 ***")
        assertContains(newest, "I Exam: 문항 3")
        assertContains(core.report(), "crash 2 ***")
        assertFalse("crash 1 " in core.report())
    }

    @Test
    fun curlQuotesEveryPartSoItPastesIntoAShell() {
        val call = DebugCall(
            time = clock.now,
            method = "POST",
            url = "https://x.dev/v1",
            requestHeaders = listOf("Content-Type" to "application/json"),
            requestBody = "{\"q\":\"it's\"}",
            status = 200,
            duration = 120.milliseconds,
        )

        assertEquals(
            "curl -X POST 'https://x.dev/v1' -H 'Content-Type: application/json' --data-raw '{\"q\":\"it'\\''s\"}'",
            call.curl(),
        )
    }
}

package io.github.nosorae.aiopic.libs.debug

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

internal class DebugLog(val time: Instant, val level: DebugLevel, val tag: String, val message: String, val error: String?)

/** 네트워크 호출 하나. [status] 가 null 이면 응답을 못 받았고 [error] 에 이유가 있다. */
internal data class DebugCall(
    val time: Instant,
    val method: String,
    val url: String,
    val requestHeaders: List<Pair<String, String>>,
    val requestBody: String,
    val status: Int?,
    val duration: Duration,
    val responseHeaders: List<Pair<String, String>> = emptyList(),
    val responseBody: String = "",
    val error: String? = null,
)

/** 기록을 모으고 보고서를 만든다. 플랫폼 일(저장, 기기 정보)은 [DebugPlatform] 에 맡긴다. */
internal class DebugCore(val clock: Clock) {
    @Volatile private var config = DebugConfig()

    @Volatile private var platform: DebugPlatform = MemoryPlatform()

    // ponytail: 로그 한 줄마다 목록을 복사한다(최대 2,000개 참조). 로그가 초당 수백 줄이 되면 잠금 있는 배열 링 버퍼로 바꾼다.
    private val _logs = MutableStateFlow<List<DebugLog>>(emptyList())
    val logs: StateFlow<List<DebugLog>> = _logs
    private val _calls = MutableStateFlow<List<DebugCall>>(emptyList())
    val calls: StateFlow<List<DebugCall>> = _calls
    private val _switches = MutableStateFlow<Map<String, DebugSwitch>>(emptyMap())
    val switches: StateFlow<Map<String, DebugSwitch>> = _switches
    private val _panels = MutableStateFlow<Map<String, suspend () -> List<Pair<String, String>>>>(emptyMap())
    val panels: StateFlow<Map<String, suspend () -> List<Pair<String, String>>>> = _panels

    fun install(config: DebugConfig, platform: DebugPlatform) {
        this.config = config
        this.platform = platform
    }

    fun log(level: DebugLevel, tag: String, message: String, error: Throwable? = null) {
        val entry = DebugLog(clock.now(), level, tag, redact(message), error?.let { redact(it.stackTraceToString()) })
        val max = config.maxLogs
        _logs.update { (it + entry).takeLast(max) }
    }

    /** 키를 가리고 본문을 자른 뒤 저장한다. 그래서 메모리에도 공유 파일에도 키와 큰 본문이 남지 않는다. */
    fun recordCall(call: DebugCall) {
        val hidden = DEFAULT_REDACT_HEADERS + config.redactHeaders.map { it.lowercase() }
        fun headers(list: List<Pair<String, String>>) = list.map { (name, value) -> name to if (name.lowercase() in hidden) "***" else redact(value) }
        val stored = call.copy(
            url = redact(call.url),
            requestHeaders = headers(call.requestHeaders),
            requestBody = body(call.requestBody),
            responseHeaders = headers(call.responseHeaders),
            responseBody = body(call.responseBody),
            error = call.error?.let(::redact),
        )
        val max = config.maxCalls
        _calls.update { (it + stored).takeLast(max) }
    }

    private fun body(text: String): String {
        val max = config.maxBodyBytes
        val size = text.encodeToByteArray().size
        if (size <= max) return redact(text)
        // 수 MB 본문 전체에 정규식을 돌리지 않게 상한 근처만 가린다. 여유를 키 길이보다 길게 둬서 잘린 끝에 키 반쪽이 남지 않는다.
        // ponytail: 앱이 더한 패턴이 REDACT_MARGIN 보다 길면 끝에 걸친 값이 반만 가려진다. 그런 패턴이 생기면 여유를 늘린다.
        val head = redact(text.take(max + REDACT_MARGIN)).encodeToByteArray()
        return head.decodeToString(0, minOf(max, head.size)) + "\n(잘림: 원래 $size 바이트)"
    }

    fun switch(key: String, title: String, default: Boolean = false): DebugSwitch {
        _switches.update { if (key in it) it else it + (key to DebugSwitch(this, key, title, default)) }
        return _switches.value.getValue(key)
    }

    fun switchEnabled(key: String, default: Boolean): Boolean = platform.readSwitch(key) ?: default

    fun setSwitch(key: String, enabled: Boolean) = platform.writeSwitch(key, enabled)

    fun panel(title: String, rows: suspend () -> List<Pair<String, String>>) {
        _panels.update { it + (title to rows) }
    }

    fun open() = platform.open()

    fun info(): List<Pair<String, String>> = platform.info()

    /** 최근 것부터 (파일 이름, 내용). */
    fun crashes(): List<Pair<String, String>> = platform.crashNames().sortedDescending().map { it to platform.readCrash(it) }

    fun deleteCrash(name: String) = platform.deleteCrash(name)

    /** 처리 안 된 예외가 난 순간의 기록을 파일로 남긴다. 파일은 [MAX_CRASHES] 개까지 두고 오래된 것부터 지운다. */
    fun recordCrash(error: Throwable) {
        val now = clock.now()
        val text = "# Crash $now\n${redact(error.stackTraceToString())}\n" + switchesText() + logsText()
        platform.writeCrash("crash-${now.toEpochMilliseconds()}.txt", text)
        platform.crashNames().sorted().dropLast(MAX_CRASHES).forEach(platform::deleteCrash)
    }

    /** 공유용 텍스트 보고서. 앱 패널이 실패해도 보고서는 만든다. */
    suspend fun report(): String = buildString {
        append("# Info\n")
        platform.info().forEach { (name, value) -> append("$name: $value\n") }
        append(switchesText())
        panelRows().forEach { (name, value) -> append(if (name.startsWith("## ")) "$name\n" else "$name: $value\n") }
        crashes().forEach { (_, text) -> append("\n$text\n") }
        append(logsText())
        append(callsText())
    }

    /** 앱 패널을 읽어 가린 줄로 돌려준다. 패널 제목은 `"## 제목" to ""` 줄이다. 화면과 보고서가 같이 써서 화면에도 키 원문이 안 보인다. */
    suspend fun panelRows(): List<Pair<String, String>> = panels.value.flatMap { (title, rows) ->
        val lines = try {
            rows()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            listOf("error" to (e.message ?: e::class.simpleName.orEmpty()))
        }
        listOf("## $title" to "") + lines.map { (name, value) -> redact(name) to redact(value) }
    }

    private fun callsText() = "# Network\n" + calls.value.joinToString("") { call ->
        val head = "## ${call.time} ${call.method} ${call.url} ${call.status ?: "실패"} ${call.duration}\n"
        fun lines(mark: String, headers: List<Pair<String, String>>, body: String) = headers.joinToString("") { (name, value) -> "$mark $name: $value\n" } +
            body.lineSequence().filter { it.isNotEmpty() }.joinToString("") { "$mark $it\n" }
        head + lines(">", call.requestHeaders, call.requestBody) + lines("<", call.responseHeaders, call.responseBody) +
            call.error?.let { "! $it\n" }.orEmpty()
    }

    private fun switchesText() = "# Switches\n" + _switches.value.values.joinToString("") {
        "${it.title} (${it.key}): ${if (it.enabled) "on" else "off"}\n"
    }

    private fun logsText() = "# Logs\n" + logs.value.joinToString("") { log ->
        "${log.time} ${log.level.name.first()} ${log.tag}: ${log.message}\n" + log.error.orEmpty().lineSequence()
            .filter { it.isNotBlank() }.joinToString("") { "    $it\n" }
    }

    private fun redact(text: String): String = redact(text, config.redactPatterns)

    companion object {
        const val MAX_CRASHES = 5
        const val REDACT_MARGIN = 256
    }
}

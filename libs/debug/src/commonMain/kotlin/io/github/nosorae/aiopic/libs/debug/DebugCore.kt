package io.github.nosorae.aiopic.libs.debug

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Instant

internal class DebugLog(val time: Instant, val level: DebugLevel, val tag: String, val message: String, val error: String?)

/** 기록을 모으고 보고서를 만든다. 플랫폼 일(저장, 기기 정보)은 [DebugPlatform] 에 맡긴다. */
internal class DebugCore(private val clock: Clock) {
    @Volatile private var config = DebugConfig()

    @Volatile private var platform: DebugPlatform = MemoryPlatform()

    // ponytail: 로그 한 줄마다 목록을 복사한다(최대 2,000개 참조). 로그가 초당 수백 줄이 되면 잠금 있는 배열 링 버퍼로 바꾼다.
    private val _logs = MutableStateFlow<List<DebugLog>>(emptyList())
    val logs: StateFlow<List<DebugLog>> = _logs
    private val switches = MutableStateFlow<Map<String, DebugSwitch>>(emptyMap())
    private val panels = MutableStateFlow<Map<String, suspend () -> List<Pair<String, String>>>>(emptyMap())

    fun install(config: DebugConfig, platform: DebugPlatform) {
        this.config = config
        this.platform = platform
    }

    fun log(level: DebugLevel, tag: String, message: String, error: Throwable? = null) {
        val entry = DebugLog(clock.now(), level, tag, redact(message), error?.let { redact(it.stackTraceToString()) })
        val max = config.maxLogs
        _logs.update { (it + entry).takeLast(max) }
    }

    fun switch(key: String, title: String, default: Boolean = false): DebugSwitch {
        switches.update { if (key in it) it else it + (key to DebugSwitch(this, key, title, default)) }
        return switches.value.getValue(key)
    }

    fun switchEnabled(key: String, default: Boolean): Boolean = platform.readSwitch(key) ?: default

    fun setSwitch(key: String, enabled: Boolean) = platform.writeSwitch(key, enabled)

    fun panel(title: String, rows: suspend () -> List<Pair<String, String>>) {
        panels.update { it + (title to rows) }
    }

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
        panels.value.forEach { (title, rows) ->
            append("## $title\n")
            try {
                rows().forEach { (name, value) -> append("$name: ${redact(value)}\n") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                append("${e.message ?: e::class.simpleName}\n")
            }
        }
        platform.crashNames().sortedDescending().forEach { append("\n${platform.readCrash(it)}\n") }
        append(logsText())
    }

    private fun switchesText() = "# Switches\n" + switches.value.values.joinToString("") {
        "${it.title} (${it.key}): ${if (it.enabled) "on" else "off"}\n"
    }

    private fun logsText() = "# Logs\n" + logs.value.joinToString("") { log ->
        "${log.time} ${log.level.name.first()} ${log.tag}: ${log.message}\n" + log.error.orEmpty().lineSequence()
            .filter { it.isNotBlank() }.joinToString("") { "    $it\n" }
    }

    private fun redact(text: String): String = redact(text, config.redactPatterns)

    companion object {
        const val MAX_CRASHES = 5
    }
}

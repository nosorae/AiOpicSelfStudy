package io.github.nosorae.aiopic.libs.debug

/** 릴리스 빌드용 빈 몸. 공개 API 는 libs/debug 와 같고 아무것도 기록하지 않는다. 스위치는 늘 기본값이다. */
public object DebugTools {
    public val installed: Boolean get() = false

    public fun log(level: DebugLevel, tag: String, message: String, error: Throwable? = null) {}

    public fun panel(title: String, rows: suspend () -> List<Pair<String, String>>) {}

    public fun switch(key: String, title: String, default: Boolean = false): DebugSwitch = DebugSwitch(default)

    public fun open() {}
}

public enum class DebugLevel { Debug, Info, Warn, Error }

public class DebugSwitch internal constructor(public val enabled: Boolean)

public class DebugConfig(
    public val redactPatterns: List<Regex> = emptyList(),
    public val maxLogs: Int = 2_000,
    public val redactHeaders: Set<String> = emptySet(),
    public val maxCalls: Int = 200,
    public val maxBodyBytes: Int = 16 * 1024,
)

package io.github.nosorae.aiopic.libs.debug

private val TRUNCATION_NOTE = Regex("\n\\(잘림: 원래 \\d+ 바이트\\)$")

/**
 * JSON 본문을 두 칸 들여쓰기로 편다. `{` 나 `[` 로 시작하지 않으면 그대로 돌려준다.
 * 잘린 본문([DebugCore] 가 끝에 원래 크기를 적은 것)도 앞부분만 펴고 끝의 안내는 그대로 둔다.
 * 문법을 검사하지 않고 글자를 한 번 훑기만 해서 깨진 JSON 에도 멈추지 않는다.
 */
internal fun prettyJson(text: String): String {
    val trimmed = text.trimStart()
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return text
    val note = TRUNCATION_NOTE.find(trimmed)?.value.orEmpty()
    val json = trimmed.removeSuffix(note)
    val out = StringBuilder()
    var depth = 0
    var inString = false
    var escaped = false
    fun newline() = out.append('\n').append("  ".repeat(depth))
    json.forEachIndexed { i, ch ->
        if (inString) {
            out.append(ch)
            when {
                escaped -> escaped = false
                ch == '\\' -> escaped = true
                ch == '"' -> inString = false
            }
            return@forEachIndexed
        }
        when (ch) {
            '"' -> {
                inString = true
                out.append(ch)
            }

            '{', '[' -> {
                out.append(ch)
                val next = (i + 1 until json.length).firstOrNull { !json[it].isWhitespace() }?.let(json::get)
                // 빈 묶음 `{}`, `[]` 는 한 줄로 둔다.
                if (next != '}' && next != ']') {
                    depth++
                    newline()
                }
            }

            '}', ']' -> {
                if (out.last() != '{' && out.last() != '[') {
                    depth = (depth - 1).coerceAtLeast(0)
                    newline()
                }
                out.append(ch)
            }

            ',' -> {
                out.append(ch)
                newline()
            }

            ':' -> out.append(": ")

            else -> if (!ch.isWhitespace()) out.append(ch)
        }
    }
    return out.append(note).toString()
}

/** 사람이 읽는 크기. 1,000 단위로 B, KB, MB 를 붙이고 소수 한 자리까지 쓴다. */
internal fun formatBytes(bytes: Int): String = when {
    bytes < 1_000 -> "$bytes B"
    bytes < 1_000_000 -> "${oneDecimal(bytes / 1_000.0)} KB"
    else -> "${oneDecimal(bytes / 1_000_000.0)} MB"
}

private fun oneDecimal(value: Double): String {
    val tenths = kotlin.math.round(value * 10).toLong()
    return "${tenths / 10}.${tenths % 10}"
}

/** 주소를 (호스트, 경로와 쿼리) 로 나눈다. 목록에서 경로를 앞에 두고 호스트를 작게 쓰려고 쓴다. 주소가 아니면 호스트는 비운다. */
internal fun splitUrl(url: String): Pair<String, String> {
    val scheme = url.indexOf("://")
    if (scheme < 0) return "" to url
    val rest = url.substring(scheme + 3)
    val slash = rest.indexOfFirst { it == '/' || it == '?' }
    return if (slash < 0) rest to "/" else rest.substring(0, slash) to rest.substring(slash).let { if (it.startsWith("?")) "/$it" else it }
}

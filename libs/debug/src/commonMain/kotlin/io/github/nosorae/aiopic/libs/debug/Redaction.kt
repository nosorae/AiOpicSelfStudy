package io.github.nosorae.aiopic.libs.debug

private val googleApiKey = Regex("AIza[0-9A-Za-z_-]{35}")

// 이 앱이 쓰는 Gemini 키 형식(docs/specs/product.md S14).
private val geminiKey = Regex("""AQ\.[0-9A-Za-z_-]{20,}""")
private val keyHeader = Regex("""(x-goog-api-key\s*:\s*)\S+""", RegexOption.IGNORE_CASE)
private val keyQuery = Regex("""([?&](?:key|api_key|access_token)=)[^&\s#]+""", RegexOption.IGNORE_CASE)

/** 공유 파일에 키가 남지 않게 보여 줄 때가 아니라 저장할 때 가린다. 가린 값은 `***` 이다. */
internal fun redact(text: String, extra: List<Regex>): String {
    var out = keyQuery.replace(text) { it.groupValues[1] + "***" }
    out = keyHeader.replace(out) { it.groupValues[1] + "***" }
    (listOf(googleApiKey, geminiKey) + extra).forEach { out = it.replace(out, "***") }
    return out
}

/** 값을 통째로 가리는 헤더 이름(소문자). 키와 세션이 들어가는 헤더다. */
internal val DEFAULT_REDACT_HEADERS = setOf("x-goog-api-key", "authorization", "proxy-authorization", "cookie", "set-cookie", "x-api-key")

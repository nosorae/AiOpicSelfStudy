package io.github.nosorae.aiopic.libs.debug

private val googleApiKey = Regex("AIza[0-9A-Za-z_-]{35}")
private val keyQuery = Regex("""([?&](?:key|api_key|access_token)=)[^&\s#]+""", RegexOption.IGNORE_CASE)

/** 공유 파일에 키가 남지 않게 보여 줄 때가 아니라 저장할 때 가린다. 가린 값은 `***` 이다. */
internal fun redact(text: String, extra: List<Regex>): String {
    var out = keyQuery.replace(text) { it.groupValues[1] + "***" }
    (listOf(googleApiKey) + extra).forEach { out = it.replace(out, "***") }
    return out
}

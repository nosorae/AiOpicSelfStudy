package io.github.nosorae.aiopic.libs.debug

import kotlin.test.Test
import kotlin.test.assertEquals

class DebugFormatTest {
    @Test
    fun indentsJsonAndLeavesStringsAlone() {
        val json = """{"a":1,"b":[true,null],"c":{"d":"x, {y}: z"},"e":[]}"""

        assertEquals(
            """
            {
              "a": 1,
              "b": [
                true,
                null
              ],
              "c": {
                "d": "x, {y}: z"
              },
              "e": []
            }
            """.trimIndent(),
            prettyJson(json),
        )
    }

    @Test
    fun keepsEscapedQuotesInsideStrings() {
        assertEquals("{\n  \"q\": \"say \\\"hi\\\", ok\"\n}", prettyJson("""{"q":"say \"hi\", ok"}"""))
    }

    @Test
    fun leavesNonJsonAsItIs() {
        assertEquals("bad key body", prettyJson("bad key body"))
        assertEquals("", prettyJson(""))
    }

    @Test
    fun indentsTheHeadOfACutBodyAndKeepsTheNote() {
        val cut = "{\"a\":[1,\"xx\n(잘림: 원래 900 바이트)"

        assertEquals("{\n  \"a\": [\n    1,\n    \"xx\n(잘림: 원래 900 바이트)", prettyJson(cut))
    }

    @Test
    fun formatsBytesForPeople() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("999 B", formatBytes(999))
        assertEquals("1.2 KB", formatBytes(1_234))
        assertEquals("3.4 MB", formatBytes(3_400_000))
    }

    @Test
    fun splitsHostFromPathAndQuery() {
        assertEquals("x.dev" to "/v1/models?key=***", splitUrl("https://x.dev/v1/models?key=***"))
        assertEquals("x.dev:8080" to "/", splitUrl("http://x.dev:8080"))
        assertEquals("" to "not a url", splitUrl("not a url"))
    }
}

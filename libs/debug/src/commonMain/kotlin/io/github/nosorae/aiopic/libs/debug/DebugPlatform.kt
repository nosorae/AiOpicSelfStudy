package io.github.nosorae.aiopic.libs.debug

/** 스위치와 크래시 파일 저장, 기기 정보. 플랫폼마다 구현하고 테스트와 install 전에는 [MemoryPlatform] 을 쓴다. */
internal interface DebugPlatform {
    fun info(): List<Pair<String, String>>
    fun readSwitch(key: String): Boolean?
    fun writeSwitch(key: String, enabled: Boolean)
    fun crashNames(): List<String>
    fun readCrash(name: String): String
    fun writeCrash(name: String, text: String)
    fun deleteCrash(name: String)
}

internal class MemoryPlatform(private val info: List<Pair<String, String>> = emptyList()) : DebugPlatform {
    private val switches = mutableMapOf<String, Boolean>()
    private val crashes = mutableMapOf<String, String>()

    override fun info() = info
    override fun readSwitch(key: String) = switches[key]
    override fun writeSwitch(key: String, enabled: Boolean) {
        switches[key] = enabled
    }
    override fun crashNames() = crashes.keys.toList()
    override fun readCrash(name: String) = crashes.getValue(name)
    override fun writeCrash(name: String, text: String) {
        crashes[name] = text
    }
    override fun deleteCrash(name: String) {
        crashes.remove(name)
    }
}

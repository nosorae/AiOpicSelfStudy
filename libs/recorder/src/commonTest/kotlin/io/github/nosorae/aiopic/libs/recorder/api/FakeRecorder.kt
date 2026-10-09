package io.github.nosorae.aiopic.libs.recorder.api

/** 테스트용. 파일을 쓰지 않는다. 시작할 때마다 조각 하나를 경로로만 만든다. */
class FakeRecorder(private val micAvailable: Boolean = true) : Recorder {
    private val folders = mutableMapOf<String, MutableList<String>>()
    private var current: String? = null

    override suspend fun start(directory: String): Boolean {
        if (!micAvailable) return false
        val chunks = folders.getOrPut(directory) { mutableListOf() }
        chunks += "$directory/${chunks.size}.m4a"
        current = directory
        return true
    }

    override suspend fun stop(): List<String> = chunks(checkNotNull(current) { "녹음 중이 아니다" }).also { current = null }

    override suspend fun chunks(directory: String): List<String> = folders[directory].orEmpty().toList()

    override suspend fun merge(chunks: List<String>, output: String): Unit = Unit
}

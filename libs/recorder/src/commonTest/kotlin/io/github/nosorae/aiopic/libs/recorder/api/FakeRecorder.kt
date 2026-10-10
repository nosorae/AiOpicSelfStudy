package io.github.nosorae.aiopic.libs.recorder.api

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 테스트용. 파일을 쓰지 않는다. 시작할 때마다 조각 하나를 경로로만 만들고, 조각 길이는 모두 [chunkLength] 로 친다.
 * [writeFails] 면 멈출 때 조각 쓰기에 실패했다고 알린다.
 */
class FakeRecorder(
    private val micAvailable: Boolean = true,
    private val chunkLength: Duration = 5.seconds,
    private val writeFails: Boolean = false,
) : Recorder {
    private val folders = mutableMapOf<String, MutableList<String>>()
    private var current: String? = null

    override suspend fun start(directory: String): RecordingFailure? {
        if (!micAvailable) return RecordingFailure.MicUnavailable
        val chunks = folders.getOrPut(directory) { mutableListOf() }
        chunks += "$directory/${chunks.size}.m4a"
        current = directory
        return null
    }

    override suspend fun stop(): StoppedRecording {
        val chunks = chunks(checkNotNull(current) { "녹음 중이 아니다" }).also { current = null }
        return StoppedRecording(chunks, if (writeFails) RecordingFailure.WriteFailed else null)
    }

    override suspend fun chunks(directory: String): List<String> = folders[directory].orEmpty().toList()

    /** 가짜는 start 한 번에 조각 하나라서 첫 조각을 뺀 모든 조각이 이어 녹음의 시작이다. */
    override suspend fun merge(chunks: List<String>, output: String): List<Duration> = (1 until chunks.size).map { chunkLength * it }
}

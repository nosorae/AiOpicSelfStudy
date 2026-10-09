package io.github.nosorae.aiopic.libs.recorder.api

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSFileManager
import platform.Foundation.NSLock
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 답변 하나를 [chunkLength] 단위 m4a 조각으로 쓴다(F3.2). 마이크는 답변 내내 열어 두고 쓰는 파일만 바꾼다.
 * 조각 이름은 `0000.m4a` 처럼 녹음한 순서 번호라서 앱이 죽었다 켜져도 이름 순으로 목록을 되찾는다(F3.3).
 * 다시 [start] 했거나 끼어듦이 끝나 새로 쓰기 시작한 조각은 `-resume` 을 붙여 [merge] 가 그 위치를 찾게 한다(F3.4).
 */
@OptIn(ExperimentalForeignApi::class)
public class IosRecorder internal constructor(private val mic: MicInput, private val chunkLength: Duration) : Recorder {
    public constructor(chunkLength: Duration = 5.seconds) : this(EngineMicInput(), chunkLength)

    // 버퍼는 오디오 스레드에서, 끼어듦 알림과 start, stop 은 다른 스레드에서 온다.
    private val lock = NSLock()
    private var directory: String? = null
    private var file: AVAudioFile? = null
    private var framesInFile = 0.0
    private var segmentStart = false
    private var interruptions: NSObjectProtocol? = null

    override suspend fun start(directory: String): Boolean {
        NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
        locked {
            this.directory = directory
            segmentStart = true
        }
        if (!mic.start(::write)) {
            locked { this.directory = null }
            return false
        }
        interruptions = NSNotificationCenter.defaultCenter.addObserverForName(AVAudioSessionInterruptionNotification, null, null, ::onInterruption)
        return true
    }

    override suspend fun stop(): List<String> {
        mic.stop()
        interruptions?.let(NSNotificationCenter.defaultCenter::removeObserver)
        interruptions = null
        val stopped = locked {
            closeChunk()
            directory.also { directory = null }
        }
        return chunks(checkNotNull(stopped) { "녹음 중이 아니다" })
    }

    override suspend fun chunks(directory: String): List<String> = names(directory).map { "$directory/$it" }

    override suspend fun merge(chunks: List<String>, output: String): List<Duration> {
        var merged: AVAudioFile? = null
        var frames = 0.0
        val resumes = mutableListOf<Duration>()
        for (path in chunks) {
            // 앱이 죽을 때 쓰던 조각은 닫히지 않아 열리지 않는다. 그 조각만 잃는다.
            // AVAudioFile 은 열지 못하면 nil 을 돌려주고 Kotlin 생성자는 그걸 NullPointerException 으로 바꾼다.
            val chunk = runCatching { AVAudioFile(forReading = NSURL.fileURLWithPath(path), error = null) }.getOrNull() ?: continue
            val format = chunk.processingFormat
            // ponytail: 조각마다 형식이 같다고 본다. 답변 도중 블루투스로 바뀌어 표본율이 달라지면 변환이 필요하다.
            val out = merged ?: AVAudioFile(
                forWriting = NSURL.fileURLWithPath(output),
                settings = chunk.fileFormat.settings,
                commonFormat = format.commonFormat,
                interleaved = format.interleaved,
                error = null,
            ).also { merged = it }
            if (path.endsWith(RESUME) && frames > 0) resumes += (frames / format.sampleRate).seconds
            val buffer = AVAudioPCMBuffer(format, frameCapacity = chunk.length.toUInt())
            if (!chunk.readIntoBuffer(buffer, error = null)) continue
            out.writeFromBuffer(buffer, error = null)
            frames += buffer.frameLength.toDouble()
        }
        merged?.close()
        return resumes
    }

    private fun write(buffer: AVAudioPCMBuffer) = locked {
        val directory = directory ?: return@locked
        val out = file ?: openChunk(directory, buffer.format)?.also { file = it } ?: return@locked
        out.writeFromBuffer(buffer, error = null)
        framesInFile += buffer.frameLength.toDouble()
        if (framesInFile >= chunkLength.inWholeMilliseconds * buffer.format.sampleRate / 1_000) closeChunk()
    }

    private fun onInterruption(notification: NSNotification?) {
        when ((notification?.userInfo?.get(AVAudioSessionInterruptionTypeKey) as? NSNumber)?.unsignedLongLongValue) {
            AVAudioSessionInterruptionTypeBegan -> {
                mic.stop()
                locked {
                    closeChunk()
                    segmentStart = true
                }
            }

            // 끝났다는 알림이 오면 마이크를 다시 연다. 다음 버퍼가 새 조각을 연다.
            AVAudioSessionInterruptionTypeEnded -> if (locked { directory } != null) mic.start(::write)
        }
    }

    /** 새로 쓰기 시작한 첫 조각인데 폴더에 조각이 이미 있으면 이어 녹음의 시작이라 `-resume` 을 붙인다. */
    private fun openChunk(directory: String, format: AVAudioFormat): AVAudioFile? {
        val existing = names(directory)
        val name = existing.size.toString().padStart(4, '0') + if (segmentStart && existing.isNotEmpty()) RESUME else ".m4a"
        segmentStart = false
        return runCatching {
            AVAudioFile(
                forWriting = NSURL.fileURLWithPath("$directory/$name"),
                settings = mapOf<Any?, Any?>(
                    AVFormatIDKey to kAudioFormatMPEG4AAC,
                    AVSampleRateKey to format.sampleRate,
                    AVNumberOfChannelsKey to format.channelCount,
                ),
                commonFormat = format.commonFormat,
                interleaved = format.interleaved,
                error = null,
            )
        }.getOrNull()
    }

    private fun closeChunk() {
        file?.close()
        file = null
        framesInFile = 0.0
    }

    private fun names(directory: String): List<String> = NSFileManager.defaultManager.contentsOfDirectoryAtPath(directory, error = null).orEmpty()
        .map { it as String }
        .filter { CHUNK.matches(it) }
        .sorted()

    private inline fun <T> locked(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }

    private companion object {
        const val RESUME = "-resume.m4a"
        val CHUNK = Regex("""\d{4}(-resume)?\.m4a""")
    }
}

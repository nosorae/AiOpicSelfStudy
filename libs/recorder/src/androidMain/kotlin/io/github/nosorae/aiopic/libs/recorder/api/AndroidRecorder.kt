package io.github.nosorae.aiopic.libs.recorder.api

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File
import java.nio.ByteBuffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

/**
 * MediaRecorder 하나로 답변 하나를 녹음하고, 파일이 일정 크기에 닿으면 [MediaRecorder.setNextOutputFile] 로 다음 조각으로 넘어간다.
 * 녹음기를 멈추지 않으니 조각 사이에 빠지는 소리가 없다.
 *
 * 조각 이름은 `00007.m4a` 처럼 녹음 순서 번호다. 녹음기를 새로 연 첫 조각(start, 끼어듦 뒤)은 `00007-r.m4a` 로 표시해서
 * 앱이 죽었다 켜진 뒤에도 [merge] 가 이어 녹음 위치를 알 수 있게 한다.
 * 전화처럼 다른 쪽이 오디오 포커스를 잠깐 가져가면 그동안 녹음을 멈추고, 돌려받으면 새 조각으로 다시 녹음한다(F3.4).
 */
public class AndroidRecorder(context: Context) : Recorder {
    private val context = context.applicationContext
    private val audio = this.context.getSystemService(AudioManager::class.java)
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
        .setOnAudioFocusChangeListener(::onFocusChange, Handler(Looper.getMainLooper()))
        .build()
    private var directory: File? = null
    private var recorder: MediaRecorder? = null

    // 오디오 포커스 콜백과 조각 넘김 콜백이 메인 스레드에서 같은 상태를 건드리므로 상태를 바꾸는 곳은 모두 this 로 잠근다.
    override suspend fun start(directory: String): Boolean = synchronized(this) {
        val dir = File(directory).apply { mkdirs() }
        try {
            context.startForegroundService(Intent(context, RecordingService::class.java))
        } catch (_: RuntimeException) {
            // 백그라운드에서는 마이크 서비스를 시작할 수 없다. 녹음이 도중에 끊길 수 있으니 시작하지 않는다.
            return false
        }
        this.directory = dir
        audio.requestAudioFocus(focus)
        if (!open(dir)) {
            finish()
            return false
        }
        true
    }

    override suspend fun stop(): List<String> {
        val dir = synchronized(this) { checkNotNull(directory) { "녹음 중이 아니다" }.also { finish() } }
        return chunks(dir.path)
    }

    private fun finish() {
        close()
        audio.abandonAudioFocusRequest(focus)
        context.stopService(Intent(context, RecordingService::class.java))
        directory = null
    }

    override suspend fun chunks(directory: String): List<String> = File(directory).listFiles { file -> CHUNK.matches(file.name) }.orEmpty()
        .sortedBy { it.name }
        // 쓰던 중에 죽은 조각은 파일 끝 정보가 없어 재생할 수 없다. 그 조각 하나만 버린다(F3.2).
        .filter { playable(it.path) }
        .map { it.path }

    override suspend fun merge(chunks: List<String>, output: String): List<Duration> {
        val resumes = mutableListOf<Duration>()
        val muxer = MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
        val info = MediaCodec.BufferInfo()
        var track = -1
        var offsetUs = 0L
        try {
            for ((index, path) in chunks.withIndex()) {
                if (index > 0 && path.endsWith(RESUMED)) resumes += offsetUs.microseconds
                val extractor = MediaExtractor().apply { setDataSource(path) }
                try {
                    extractor.selectTrack(0)
                    if (track < 0) {
                        track = muxer.addTrack(extractor.getTrackFormat(0))
                        muxer.start()
                    }
                    while (true) {
                        info.size = extractor.readSampleData(buffer, 0)
                        if (info.size < 0) break
                        info.offset = 0
                        info.presentationTimeUs = offsetUs + extractor.sampleTime
                        info.flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                        muxer.writeSampleData(track, buffer, info)
                        extractor.advance()
                    }
                    offsetUs += extractor.getTrackFormat(0).getLong(MediaFormat.KEY_DURATION)
                } finally {
                    extractor.release()
                }
            }
        } finally {
            if (track >= 0) muxer.stop()
            muxer.release()
        }
        return resumes
    }

    private fun open(dir: File): Boolean {
        val next = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        next.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(SAMPLE_RATE)
            setAudioEncodingBitRate(BIT_RATE)
            // ponytail: 길이 대신 크기로 조각을 나눈다. 고정 비트레이트라 약 5초다. 정확한 길이가 필요하면 AudioRecord 와 MediaCodec 으로 바꾼다.
            setMaxFileSize(CHUNK_BYTES)
            setOutputFile(newChunk(dir, resumed = true))
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING) nextChunk()
            }
        }
        return try {
            next.prepare()
            next.start()
            recorder = next
            true
        } catch (_: Exception) {
            // 마이크 권한이 없거나 다른 앱이 마이크를 쥐고 있다.
            next.release()
            false
        }
    }

    @Synchronized
    private fun nextChunk() {
        val dir = directory ?: return
        recorder?.setNextOutputFile(newChunk(dir, resumed = false))
    }

    private fun close() {
        val current = recorder ?: return
        recorder = null
        // 소리가 하나도 안 들어온 채 멈추면 RuntimeException 이다. 그 조각은 재생할 수 없어 chunks 가 거른다.
        runCatching { current.stop() }
        current.release()
    }

    @Synchronized
    private fun onFocusChange(change: Int) {
        val dir = directory ?: return
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> close()
            AudioManager.AUDIOFOCUS_GAIN -> if (recorder == null) open(dir)
        }
    }

    private fun newChunk(dir: File, resumed: Boolean): File {
        val last = dir.listFiles { file -> CHUNK.matches(file.name) }.orEmpty().maxOfOrNull { it.name.take(DIGITS).toInt() } ?: -1
        val name = "%0${DIGITS}d".format(last + 1) + if (resumed) RESUMED else ".m4a"
        return File(dir, name)
    }

    private fun playable(path: String): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0) > 0
        } catch (_: RuntimeException) {
            false
        } finally {
            retriever.release()
        }
    }

    private companion object {
        val CHUNK = Regex("""\d{5}(-r)?\.m4a""")
        const val DIGITS = 5
        const val RESUMED = "-r.m4a"
        const val SAMPLE_RATE = 44_100
        const val BIT_RATE = 64_000
        const val CHUNK_BYTES = 5L * BIT_RATE / 8
        const val BUFFER_SIZE = 64 * 1024
    }
}

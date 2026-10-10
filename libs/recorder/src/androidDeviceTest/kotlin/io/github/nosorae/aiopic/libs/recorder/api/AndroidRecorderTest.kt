package io.github.nosorae.aiopic.libs.recorder.api

import android.Manifest
import android.content.ComponentName
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// 녹음은 실제 시간이 흘러야 조각이 생기므로 runTest 의 가상 시간 대신 runBlocking 과 Thread.sleep 을 쓴다.
@RunWith(AndroidJUnit4::class)
class AndroidRecorderTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val dir = File(context.cacheDir, "answer-test")

    @Before
    fun setUp() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
        dir.deleteRecursively()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `F3_5_녹음_서비스를_마이크_포그라운드_서비스로_선언한다`() {
        val info = context.packageManager.getServiceInfo(ComponentName(context, RecordingService::class.java), 0)

        assertTrue(info.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE != 0)
    }

    @Test
    fun `F3_2_녹음기를_끊지_않고_몇_초마다_새_조각_파일로_넘어간다`() = runBlocking {
        val recorder = AndroidRecorder(context)
        assertNull(recorder.start(dir.path))
        Thread.sleep(12_000)
        val chunks = recorder.stop().chunks

        assertTrue(chunks.size >= 2, "조각 $chunks")
        val merged = File(dir, "merged.m4a").path
        // 녹음기를 한 번만 열었으니 이어 녹음 위치가 없다.
        assertEquals(emptyList(), recorder.merge(chunks, merged))
        assertTrue(durationOf(merged) >= 10.seconds, "합친 길이 ${durationOf(merged)}")
    }

    @Test
    fun `F3_2_F3_3_프로세스가_죽은_뒤_새_녹음기가_끝난_조각만_되찾고_이어_녹음한다`() = runBlocking {
        // 프로세스가 죽은 상황: 녹음 중인 녹음기를 그대로 두고 새 녹음기로 같은 폴더를 연다.
        val dead = AndroidRecorder(context)
        assertNull(dead.start(dir.path))
        Thread.sleep(8_000)
        val restored = AndroidRecorder(context).chunks(dir.path)
        dead.stop()

        assertTrue(restored.isNotEmpty())
        // 쓰던 조각은 파일 끝 정보가 없어 재생할 수 없으므로 목록에서 빠진다.
        assertTrue(restored.size < dir.listFiles()!!.size, "되찾은 조각 $restored")
        restored.forEach { assertTrue(durationOf(it) > 0.seconds, it) }

        val recorder = AndroidRecorder(context)
        assertNull(recorder.start(dir.path))
        Thread.sleep(2_000)
        val all = recorder.stop().chunks

        assertEquals(all.sorted(), all)
        assertTrue(all.size > restored.size)
        val resumes = recorder.merge(all, File(dir, "merged.m4a").path)
        // 죽기 전에 쓴 조각(마지막 조각까지) 뒤에서 이어 녹음했다.
        assertEquals(1, resumes.size)
        assertTrue(resumes.single() >= 7.seconds, "이어 녹음 위치 $resumes")
    }

    @Test
    fun `F3_4_다른_앱이_오디오를_잠깐_가져가면_그동안_멈추고_끝나면_새_조각으로_다시_녹음한다`() = runBlocking {
        val recorder = AndroidRecorder(context)
        assertNull(recorder.start(dir.path))
        Thread.sleep(2_000)
        // 전화가 울릴 때처럼 다른 쪽이 오디오 포커스를 잠깐 가져간다.
        val audio = context.getSystemService(AudioManager::class.java)
        val call = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build())
            .build()
        assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, audio.requestAudioFocus(call))
        Thread.sleep(1_000)
        audio.abandonAudioFocusRequest(call)
        Thread.sleep(2_000)
        val chunks = recorder.stop().chunks

        val resumes = recorder.merge(chunks, File(dir, "merged.m4a").path)
        assertEquals(1, resumes.size, "조각 $chunks")
        assertTrue(resumes.single() in 1.seconds..3.seconds, "이어 녹음 위치 $resumes")
    }

    @Test
    fun `폴더를_만들_수_없으면_시작하지_못한다`() = runBlocking {
        dir.parentFile!!.mkdirs()
        dir.writeText("파일")

        assertEquals(RecordingFailure.WriteFailed, AndroidRecorder(context).start(File(dir, "answer").path))
    }

    @Test
    fun `다음_조각_파일을_만들지_못하면_멈출_때_알린다`() = runBlocking {
        val recorder = AndroidRecorder(context)
        assertNull(recorder.start(dir.path))
        // 녹음 도중 폴더가 사라지면 약 5초 뒤 다음 조각 파일을 만들 수 없다.
        dir.deleteRecursively()
        Thread.sleep(8_000)

        assertEquals(RecordingFailure.WriteFailed, recorder.stop().failure)
    }

    private fun durationOf(path: String) = MediaMetadataRetriever().run {
        try {
            setDataSource(path)
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong().milliseconds
        } finally {
            release()
        }
    }
}

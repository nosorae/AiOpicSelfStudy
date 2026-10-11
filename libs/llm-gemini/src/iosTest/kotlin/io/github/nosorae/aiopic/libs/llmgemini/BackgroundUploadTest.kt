@file:OptIn(ExperimentalForeignApi::class)

package io.github.nosorae.aiopic.libs.llmgemini

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BackgroundUploadTest {
    private val directory = NSTemporaryDirectory() + "upload-test-" + NSUUID().UUIDString
    private val url = "https://example.test/models/m:generateContent"
    private val body = """{"contents":"recording"}"""
    private val key = uploadKey(url, body)

    /** 시스템 전송 대신 시작한 요청과 보내는 중인 요청을 테스트가 정한다. */
    private class FakeTransport : BackgroundUpload.Transport {
        val started = mutableListOf<String>()
        var running = emptySet<String>()
        override fun start(key: String, url: String, apiKey: String, bodyPath: String) {
            started += key
        }
        override suspend fun running() = running
    }

    private val transport = FakeTransport()

    private fun upload(transport: BackgroundUpload.Transport = this.transport) = BackgroundUpload(directory) { transport }

    @AfterTest
    fun cleanUp() {
        NSFileManager.defaultManager.removeItemAtPath(directory, null)
    }

    private fun TestScope.post(upload: BackgroundUpload) = backgroundScope.async { upload.post(url, "test-key", body) }.also { runCurrent() }

    @Test
    fun `216 처음 보내는 요청은 시스템 전송을 시작하고 끝나면 받은 응답을 준다`() = runTest {
        val upload = upload()
        val result = post(upload)

        upload.finished(key, RawResponse(200, "done"))

        assertEquals(listOf(key), transport.started)
        assertEquals(200 to "done", result.await()!!.let { it.status to it.body })
    }

    @Test
    fun `216 같은 요청이 아직 보내지는 중이면 다시 보내지 않고 그 결과를 기다린다`() = runTest {
        transport.running = setOf(key)
        val upload = upload()
        val result = post(upload)

        upload.finished(key, RawResponse(200, "done"))

        assertEquals(emptyList(), transport.started)
        assertEquals("done", result.await()?.body)
    }

    @Test
    fun `216 기다리던 쪽이 시간 제한으로 멈춘 뒤 온 응답은 남겨 두었다가 다음 같은 요청에 다시 보내지 않고 준다`() = runTest {
        val upload = upload()
        assertEquals(null, withTimeoutOrNull(2.minutes) { upload.post(url, "test-key", body) })

        upload.finished(key, RawResponse(200, "late"))
        val again = upload.post(url, "test-key", body)

        assertEquals("late", again?.body)
        assertEquals(1, transport.started.size)
    }

    @Test
    fun `216 남겨 둔 응답은 한 번만 준다`() = runTest {
        val upload = upload()
        val first = post(upload)
        upload.finished(key, RawResponse(200, "done"))
        first.await()

        post(upload)

        assertEquals(2, transport.started.size)
    }

    @Test
    fun `216 연결하지 못하고 끝나면 null 이다`() = runTest {
        val upload = upload()
        val result = post(upload)

        upload.finished(key, null)

        assertEquals(null, result.await())
    }

    @Test
    fun `216 앱이 꺼진 사이 끝난 전송은 다시 켠 뒤 결과가 늦게 와도 다시 보내지 않는다`() = runTest {
        post(upload())
        val relaunched = FakeTransport()
        val upload = upload(relaunched)
        val result = post(upload)

        upload.finished(key, RawResponse(200, "done while away"))

        assertEquals(emptyList(), relaunched.started)
        assertEquals("done while away", result.await()?.body)
    }

    @Test
    fun `216 보낸 본문만 남고 결과가 끝내 오지 않으면 다시 보낸다`() = runTest {
        post(upload())
        val relaunched = FakeTransport()
        post(upload(relaunched))

        advanceTimeBy(11.seconds)
        runCurrent()

        assertEquals(listOf(key), relaunched.started)
    }
}

@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.github.nosorae.aiopic.libs.llmgemini

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.HTTPMethod
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskStateRunning
import platform.Foundation.NSURLSessionTaskStateSuspended
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.appendData
import platform.Foundation.create
import platform.Foundation.setValue
import platform.Foundation.writeToFile
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds

/**
 * 녹음을 실은 Gemini 요청을 iOS 백그라운드 전송으로 보낸다(#216). 앱을 내려 iOS 가 앱을 멈춰도 시스템이 전송을 끝낸다.
 *
 * 요청마다 본문과 주소로 열쇠를 만든다. 같은 녹음, 같은 프롬프트를 다시 보내면 열쇠가 같다.
 * - 같은 열쇠가 아직 보내지는 중이면 새로 보내지 않고 그 결과를 기다린다. 앱이 다시 켜져 분석 작업이 다시 돌아도 두 번 보내지 않는다
 * - 기다리던 쪽이 없을 때 온 응답(시간 제한에 멈췄거나 앱이 멈춰 있었다)은 파일로 남겨 두고, 다음 같은 요청에 보내지 않고 준다.
 *   그 응답은 분석 작업이 해석하기 전에 응답 원문 표에 저장한다
 *
 * 한 프로세스에 하나만 만들고, 앱이 실행을 마치기 전에 만든다. 앱이 꺼진 사이 끝난 전송의 결과는 같은 식별자로 세션을 다시 만들 때 온다.
 */
// ponytail: 대기와 파일은 메인 스레드 하나에서만 만진다(세션 콜백도 메인 큐). 다른 스레드에서 부르게 되면 잠금을 더한다.
public class BackgroundUpload internal constructor(private val directory: String, transport: (BackgroundUpload) -> Transport) : GeminiUpload {
    /** [identifier] 는 앱에서 하나인 백그라운드 세션 이름이다. */
    public constructor(identifier: String) : this(cachesDirectory() + "/gemini-upload", { SessionTransport(identifier, it) })

    /** 시스템 전송. 테스트는 가짜를 준다. */
    internal interface Transport {
        fun start(key: String, url: String, apiKey: String, bodyPath: String)

        /** 아직 끝나지 않은 전송의 열쇠. */
        suspend fun running(): Set<String>

        /** 앱이 꺼진 사이 끝난 전송을 앱이 다 받으면 [done] 을 부른다. */
        fun whenEventsDelivered(done: () -> Unit) {}
    }

    private val transport = transport(this)
    private val waiters = mutableMapOf<String, CompletableDeferred<Unit>>()

    init {
        NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
    }

    override suspend fun post(url: String, apiKey: String, body: String): RawResponse? {
        val key = uploadKey(url, body)
        if (key !in transport.running()) {
            // 앱이 꺼진 사이 끝난 전송은 세션을 다시 만든 뒤 결과가 온다. 보낸 본문이 남아 있으면 그 결과를 잠깐 기다린다.
            if (exists(bodyPath(key))) withTimeoutOrNull(LATE_RESULT) { finish(key) }
            take(key)?.let { return it }
            NSString.create(string = body).writeToFile(bodyPath(key), atomically = true, encoding = NSUTF8StringEncoding, error = null)
            transport.start(key, url, apiKey, bodyPath(key))
        }
        finish(key)
        return take(key)
    }

    /** 전송 하나가 끝났다. 연결하지 못했으면 [response] 가 null 이다. 받은 응답은 기다리는 쪽이 있든 없든 먼저 파일로 남긴다. */
    internal fun finished(key: String, response: RawResponse?) {
        response?.let { NSString.create(string = "${it.status}\n${it.body}").writeToFile(responsePath(key), atomically = true, encoding = NSUTF8StringEncoding, error = null) }
        NSFileManager.defaultManager.removeItemAtPath(bodyPath(key), null)
        waiters.remove(key)?.complete(Unit)
    }

    /** 앱 틀이 받은 백그라운드 전송 완료 처리기. 앱이 꺼진 사이 끝난 전송을 다 받으면 부른다. */
    public fun whenEventsDelivered(done: () -> Unit): Unit = transport.whenEventsDelivered(done)

    private suspend fun finish(key: String) = waiters.getOrPut(key) { CompletableDeferred() }.await()

    /** 남겨 둔 응답. 한 번만 준다. */
    private fun take(key: String): RawResponse? {
        val path = responsePath(key)
        val text = readText(path) ?: return null
        NSFileManager.defaultManager.removeItemAtPath(path, null)
        return RawResponse(text.substringBefore('\n').toInt(), text.substringAfter('\n'))
    }

    private fun bodyPath(key: String) = "$directory/$key.body"

    private fun responsePath(key: String) = "$directory/$key.response"

    private fun exists(path: String) = NSFileManager.defaultManager.fileExistsAtPath(path)

    private companion object {
        /** 앱이 다시 켜진 뒤 꺼진 사이 끝난 전송의 결과가 오기를 기다리는 시간. 넘으면 결과를 잃은 것으로 보고 다시 보낸다. */
        val LATE_RESULT = 10.seconds
    }
}

/** 요청 하나의 열쇠. 키는 넣지 않는다. 키를 바꿔 다시 보내도 같은 요청이다. */
internal fun uploadKey(url: String, body: String): String {
    val bytes = (url + "\n" + body).encodeToByteArray()
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    bytes.usePinned { input -> digest.usePinned { output -> CC_SHA256(input.addressOf(0), bytes.size.convert(), output.addressOf(0)) } }
    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}

private fun readText(path: String): String? = NSData.create(contentsOfFile = path)?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString() }

private fun cachesDirectory(): String = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String

/** 시스템이 맡아 보내는 백그라운드 세션. 콜백은 메인 큐로 받는다. */
private class SessionTransport(identifier: String, private val owner: BackgroundUpload) : BackgroundUpload.Transport {
    private val received = mutableMapOf<ULong, NSMutableData>()
    private var eventsDone: (() -> Unit)? = null

    /** 처리기를 받기 전에 전송 결과를 다 받았다. 받은 처리기를 바로 부른다. */
    private var delivered = false
    private val session = NSURLSession.sessionWithConfiguration(
        NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(identifier),
        SessionDelegate(::append, ::completed, ::eventsFinished),
        NSOperationQueue.mainQueue,
    )

    override fun start(key: String, url: String, apiKey: String, bodyPath: String) {
        val request = NSMutableURLRequest(NSURL(string = url))
        request.HTTPMethod = "POST"
        // 키를 주소에 넣으면 접속 기록에 남아서 헤더로 보낸다.
        request.setValue(apiKey, forHTTPHeaderField = "x-goog-api-key")
        request.setValue("application/json", forHTTPHeaderField = "Content-Type")
        val task = session.uploadTaskWithRequest(request, fromFile = NSURL.fileURLWithPath(bodyPath))
        task.taskDescription = key
        task.resume()
    }

    override suspend fun running(): Set<String> = suspendCoroutine { continuation ->
        session.getAllTasksWithCompletionHandler { tasks ->
            continuation.resume(
                tasks.orEmpty().filterIsInstance<NSURLSessionTask>()
                    .filter { it.state == NSURLSessionTaskStateRunning || it.state == NSURLSessionTaskStateSuspended }
                    .mapNotNull { it.taskDescription }.toSet(),
            )
        }
    }

    override fun whenEventsDelivered(done: () -> Unit) {
        if (delivered) done() else eventsDone = done
        delivered = false
    }

    private fun append(task: NSURLSessionTask, data: NSData) {
        received.getOrPut(task.taskIdentifier) { NSMutableData() }.appendData(data)
    }

    private fun completed(task: NSURLSessionTask, error: NSError?) {
        val data = received.remove(task.taskIdentifier)
        val key = task.taskDescription ?: return
        val status = (task.response as? NSHTTPURLResponse)?.statusCode?.toInt()
        val text = data?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString() }.orEmpty()
        owner.finished(key, if (error == null && status != null) RawResponse(status, text) else null)
    }

    private fun eventsFinished() {
        val done = eventsDone
        eventsDone = null
        delivered = done == null
        done?.invoke()
    }
}

private class SessionDelegate(
    private val onData: (NSURLSessionTask, NSData) -> Unit,
    private val onComplete: (NSURLSessionTask, NSError?) -> Unit,
    private val onEventsFinished: () -> Unit,
) : NSObject(),
    NSURLSessionDataDelegateProtocol {
    @ObjCSignatureOverride
    override fun URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveData: NSData) = onData(dataTask, didReceiveData)

    @ObjCSignatureOverride
    override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) = onComplete(task, didCompleteWithError)

    override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) = onEventsFinished()
}

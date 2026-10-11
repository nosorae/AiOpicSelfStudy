package io.github.nosorae.aiopic.libs.workqueue.api

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.darwin.dispatch_get_main_queue
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * iOS 의 [WorkQueue]. 앱이 앞에 있고 연결돼 있는 동안 [Drain] 을 부르고, 다시 불러야 할 시간이 남으면 그만큼 기다렸다 다시 부른다.
 * [online] 이 false 인 동안은 부르지 않는다.
 *
 * iOS 는 앱이 뒤로 가면 곧 멈추게 한다. [backgroundTask] 를 주면 뒤로 갈 때 네트워크가 필요한 백그라운드 처리 작업(BGProcessingTask)을
 * 예약해 시스템이 정한 때에 이어서 비운다. 식별자는 Info.plist 의 `BGTaskSchedulerPermittedIdentifiers` 에 있어야 하고,
 * 이 객체는 앱이 실행을 마치기 전에 만들어야 한다(등록 시점 제약).
 */
public class IosWorkQueue(
    private val drain: Drain,
    private val scope: CoroutineScope,
    private val online: Flow<Boolean> = networkAvailability(),
    private val backgroundTask: String? = null,
) : WorkQueue {
    private val wakes = Channel<Unit>(Channel.CONFLATED)
    private var connected = false

    init {
        scope.launch {
            online.collect {
                connected = it
                if (it) wakes.trySend(Unit)
            }
        }
        scope.launch {
            var next: Duration? = null
            while (true) {
                // 깨울 때까지, 다시 부를 시간이 있으면 그때까지 기다린다.
                if (next == null) wakes.receive() else withTimeoutOrNull(next) { wakes.receive() }
                next = if (connected) drainOnce() else null
            }
        }
        backgroundTask?.let(::registerBackgroundTask)
    }

    override fun wake() {
        wakes.trySend(Unit)
    }

    private suspend fun drainOnce(): Duration? = try {
        drain.drain()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        RETRY_AFTER_ERROR
    }

    private fun registerBackgroundTask(identifier: String) {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(identifier, usingQueue = null) { task -> task?.let { runInBackground(identifier, it) } }
        NSNotificationCenter.defaultCenter.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) { _ ->
            submit(identifier, Duration.ZERO)
        }
    }

    private fun runInBackground(identifier: String, task: BGTask) {
        val job = scope.launch {
            val next = drainOnce()
            // 다 못 비웠으면 다음 처리 작업을 예약한다. 앱이 앞으로 오면 앞 루프가 먼저 비운다.
            next?.let { submit(identifier, it) }
            task.setTaskCompletedWithSuccess(true)
        }
        // 시스템이 준 시간이 끝나면 멈춘다. 보내던 작업은 DB 에서 대기로 돌아간다.
        task.expirationHandler = {
            job.cancel()
            task.setTaskCompletedWithSuccess(false)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun submit(identifier: String, delay: Duration) {
        val request = BGProcessingTaskRequest(identifier)
        request.requiresNetworkConnectivity = true
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(delay.toDouble(DurationUnit.SECONDS))
        // ponytail: 예약 실패(시뮬레이터, 백그라운드 새로 고침 끔)는 무시한다. 다음에 앱이 앞으로 오면 앞 루프가 비운다.
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null)
    }
}

private fun networkAvailability(): Flow<Boolean> = callbackFlow {
    val monitor = nw_path_monitor_create()
    nw_path_monitor_set_update_handler(monitor) { path -> trySend(nw_path_get_status(path) == nw_path_status_satisfied) }
    nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
    nw_path_monitor_start(monitor)
    awaitClose { nw_path_monitor_cancel(monitor) }
}

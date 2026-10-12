package io.github.nosorae.aiopic.libs.workqueue.api

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
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
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIBackgroundTaskInvalid
import platform.darwin.dispatch_get_main_queue
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * iOS 의 [WorkQueue]. 앱이 앞에 있고 연결돼 있는 동안 [Drain] 을 부르고, 다시 불러야 할 시간이 남으면 그만큼 기다렸다 다시 부른다.
 * [network] 가 false 인 동안은 부르지 않는다.
 *
 * iOS 는 앱이 뒤로 가면 곧 멈추게 한다. [backgroundTask] 를 주면 뒤로 갈 때 네트워크가 필요한 백그라운드 처리 작업(BGProcessingTask)을
 * 예약해 시스템이 정한 때에 이어서 비운다. 식별자는 Info.plist 의 `BGTaskSchedulerPermittedIdentifiers` 에 있어야 하고,
 * 이 객체는 앱이 실행을 마치기 전에 만들어야 한다(등록 시점 제약).
 *
 * 비우는 동안은 [extraTime] 으로 앱이 뒤로 가도 끝낼 시간을 받아 둔다(#216). 보내던 요청이 앱을 내리자마자 끊기지 않는다.
 * 받은 시간이 끝나면 비우기를 멈추고 [RETRY_AFTER_ERROR] 뒤에 다시 비운다. 보내던 작업은 DB 에서 대기로 돌아간다.
 * 다만 [keepDraining] 이 true 면(시스템 진행 표시가 앱을 깨워 두는 동안, #229) 받은 시간만 돌려주고 비우기는 그대로 둔다.
 * 진행 표시는 계속 도는데 비우기만 멈췄다 다시 도는 일이 없게 한다.
 */
public class IosWorkQueue(
    private val drain: Drain,
    private val scope: CoroutineScope,
    private val network: Flow<Boolean> = networkAvailability(),
    private val backgroundTask: String? = null,
    /** 앱이 뒤로 가도 끝낼 시간을 받는다. 시간이 끝나면 받은 함수를 부르고, 돌려준 함수로 시간을 돌려준다. */
    private val extraTime: (onExpire: () -> Unit) -> (() -> Unit) = ::backgroundTime,
    /** 받은 시간이 끝나도 비우기를 이어 가도 되는가. 앱이 다른 방법으로 깨어 있는 동안 true 다. */
    private val keepDraining: () -> Boolean = { false },
) : WorkQueue {
    private val wakes = Channel<Unit>(Channel.CONFLATED)
    override var online: Boolean = false
        private set

    init {
        scope.launch {
            network.collect {
                online = it
                if (it) wakes.trySend(Unit)
            }
        }
        scope.launch {
            var next: Duration? = null
            while (true) {
                // 깨울 때까지, 다시 부를 시간이 있으면 그때까지 기다린다.
                if (next == null) wakes.receive() else withTimeoutOrNull(next) { wakes.receive() }
                next = if (online) drainOnce() else null
            }
        }
        backgroundTask?.let(::registerBackgroundTask)
    }

    override fun wake() {
        wakes.trySend(Unit)
    }

    private suspend fun drainOnce(): Duration? = coroutineScope {
        val work = async {
            try {
                drain.drain()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                RETRY_AFTER_ERROR
            }
        }
        val end = extraTime { if (!keepDraining()) work.cancel() }
        try {
            work.await()
        } catch (e: CancellationException) {
            // 받은 시간이 끝나 멈췄다. 대기열 자체가 멈춘 게 아니면 나중에 다시 비운다.
            ensureActive()
            RETRY_AFTER_ERROR
        } finally {
            end()
        }
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

/** 앱이 뒤로 가도 이어 돌 시간(보통 30초 안팎)을 받는다. 시간이 끝나면 [onExpire] 를 부르고 바로 돌려준다. */
private fun backgroundTime(onExpire: () -> Unit): () -> Unit {
    val app = UIApplication.sharedApplication
    var id = UIBackgroundTaskInvalid
    val end = {
        if (id != UIBackgroundTaskInvalid) app.endBackgroundTask(id)
        id = UIBackgroundTaskInvalid
    }
    id = app.beginBackgroundTaskWithName("analysis") {
        onExpire()
        end()
    }
    return end
}

private fun networkAvailability(): Flow<Boolean> = callbackFlow {
    val monitor = nw_path_monitor_create()
    nw_path_monitor_set_update_handler(monitor) { path -> trySend(nw_path_get_status(path) == nw_path_status_satisfied) }
    nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
    nw_path_monitor_start(monitor)
    awaitClose { nw_path_monitor_cancel(monitor) }
}

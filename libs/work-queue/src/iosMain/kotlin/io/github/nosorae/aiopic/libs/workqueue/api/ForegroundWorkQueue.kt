package io.github.nosorae.aiopic.libs.workqueue.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSUserDefaults
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

/**
 * iOS 의 [WorkQueue]. 대기열을 기기에 저장해 두고 앱이 앞에 떠 있는 동안 연결돼 있으면 넣은 순서대로 비운다.
 * iOS 는 앱이 뒤로 가면 곧 멈추게 해서, 넣자마자 보내고 다 못 보낸 것은 다음에 켜질 때 마저 보낸다.
 * [online] 이 false 인 동안은 보내지 않아 오프라인이 재시도 횟수를 쓰지 않는다.
 */
public class ForegroundWorkQueue(
    private val handler: WorkHandler,
    private val scope: CoroutineScope,
    private val online: Flow<Boolean> = networkAvailability(),
    // ponytail: 답변 id 와 짧은 payload 만 두는 대기열이라 DB 대신 NSUserDefaults 에 둔다. 수백 개가 쌓이면 파일로 옮긴다.
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : WorkQueue {
    /** 한 번에 하나만 보내게 막는다. 다시 보내기를 기다리는 동안에도 잡고 있다. */
    private val sending = Mutex()

    /** 저장한 목록을 읽고 고쳐 쓰는 동안만 잡는다. 보내기를 기다리는 동안에도 새 답변을 넣을 수 있어야 한다. */
    private val storage = Mutex()
    private var connected = false

    init {
        scope.launch {
            online.collect {
                connected = it
                // drain 을 여기서 기다리면 보내는 동안 끊김을 못 받으니 따로 띄운다.
                if (it) scope.launch { drain() }
            }
        }
    }

    override suspend fun enqueue(id: String, payload: String) {
        update { items -> if (items.any { it.first == id }) items else items + (id to payload) }
        scope.launch { drain() }
    }

    override suspend fun cancelAll() {
        update { emptyList() }
    }

    private suspend fun drain() = sending.withLock {
        while (connected) {
            val (id, payload) = load().firstOrNull() ?: return@withLock
            var retries = 0
            while (handler.handle(payload) == WorkResult.Retry && retries < MAX_RETRIES) {
                delay(FIRST_BACKOFF * (1 shl retries))
                retries++
                // 기다리는 사이 끊겼으면 지우지 않고 멈춘다. 다시 연결되면 collect 가 drain 을 다시 부른다(F5.6).
                if (!connected) return@withLock
                // 기다리는 사이 음성 전송 동의를 철회해 비웠으면 더 보내지 않는다(F1.3).
                if (load().none { it.first == id }) break
            }
            update { items -> items.filterNot { it.first == id } }
        }
    }

    private fun load(): List<Pair<String, String>> = defaults.arrayForKey(QUEUE_KEY).orEmpty().map {
        val item = it as List<*>
        item[0] as String to item[1] as String
    }

    private suspend fun update(change: (List<Pair<String, String>>) -> List<Pair<String, String>>) = storage.withLock {
        defaults.setObject(change(load()).map { listOf(it.first, it.second) }, QUEUE_KEY)
    }
}

internal const val QUEUE_KEY = "work-queue"

private fun networkAvailability(): Flow<Boolean> = callbackFlow {
    val monitor = nw_path_monitor_create()
    nw_path_monitor_set_update_handler(monitor) { path -> trySend(nw_path_get_status(path) == nw_path_status_satisfied) }
    nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
    nw_path_monitor_start(monitor)
    awaitClose { nw_path_monitor_cancel(monitor) }
}

package io.github.nosorae.aiopic.libs.workqueue.api

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * WorkManager 로 만든 [WorkQueue]. 고유 작업 사슬 하나에 비우기 작업을 이어 붙인다. 네트워크가 연결돼야 돌고,
 * 앱이 꺼져도 WorkManager 가 남겨 두었다가 프로세스를 깨워 [Drain] 을 부른다. 다시 불러야 할 시간이 남으면 그만큼 늦춘 작업을 뒤에 붙인다.
 *
 * [progress] 를 주면 비우는 동안 그 알림을 띄운 데이터 동기화 포그라운드 서비스로 돈다(#229). 비우기를 시작할 때 한 번 불러 구독하고,
 * 새 알림이 올 때마다 바꿔 단다. 비우기가 끝나면 WorkManager 가 알림을 내린다.
 */
public class WorkManagerWorkQueue(private val context: Context, drain: Drain, progress: (() -> Flow<Notification?>)? = null) : WorkQueue {
    init {
        DrainWorker.drain = drain
        DrainWorker.progress = progress
    }

    override fun wake(): Unit = schedule(context, Duration.ZERO)

    // 시스템이 인터넷에 닿는 것까지 확인한 연결만 온라인으로 본다. 로그인 페이지에 막힌 Wi-Fi 는 오프라인으로 기다린다.
    override val online: Boolean
        get() = context.getSystemService(ConnectivityManager::class.java).let { it.getNetworkCapabilities(it.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true }
}

internal const val CHAIN = "work-queue"

// 앞 작업이 끝나야 다음 작업이 돈다. 비우기는 한 번에 하나만 돌아야 무료 키의 분당 한도를 지킨다.
private fun schedule(context: Context, delay: Duration) {
    val request = OneTimeWorkRequestBuilder<DrainWorker>()
        .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
        .setInitialDelay(delay.toJavaDuration())
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(CHAIN, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
}

internal class DrainWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // ponytail: 비우기를 전역 하나로 둔다. 앱 조립 전에 WorkManager 가 먼저 깨우면 다시 시도로 미룬다.
        val drain = drain ?: return Result.retry()
        val next = coroutineScope {
            val shown = progress?.let { notifications -> launch(start = CoroutineStart.UNDISPATCHED) { notifications().filterNotNull().collect(::showProgress) } }
            try {
                drain.drain()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                RETRY_AFTER_ERROR
            } finally {
                shown?.cancel()
            }
        }
        // 코루틴 안에서 막지 않게 Flow 로 읽는다. 블로킹 get() 은 같은 실행기를 기다리다 멈출 수 있다.
        // 뒤에 이미 비우기가 붙어 있으면 그것이 이어서 비운다. 붙이지 않아야 사슬이 깨운 횟수만큼 늘지 않는다.
        val waiting = WorkManager.getInstance(applicationContext).getWorkInfosForUniqueWorkFlow(CHAIN).first().count { !it.state.isFinished && it.id != id }
        if (next != null && waiting == 0) schedule(applicationContext, next)
        // 실패로 끝내면 사슬 뒤 작업도 실패한다. 늘 성공으로 끝낸다.
        return Result.success()
    }

    private suspend fun showProgress(notification: Notification) {
        try {
            setForeground(ForegroundInfo(PROGRESS_NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
        } catch (e: CancellationException) {
            throw e
        } catch (_: IllegalStateException) {
            // 앱이 화면에 없을 때 깨어난 비우기는 포그라운드 서비스를 시작하지 못한다. 알림 없이 그대로 비운다.
        }
    }

    companion object {
        @Volatile
        var drain: Drain? = null

        @Volatile
        var progress: (() -> Flow<Notification?>)? = null

        private const val PROGRESS_NOTIFICATION = 2290
    }
}

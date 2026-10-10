package io.github.nosorae.aiopic.libs.workqueue.api

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.workDataOf
import kotlinx.coroutines.flow.first
import kotlin.time.toJavaDuration

/**
 * WorkManager 로 만든 [WorkQueue]. 답변 id 를 태그로 단 작업을 하나의 고유 사슬에 이어 붙여 넣은 순서대로 하나씩 보낸다.
 * 네트워크가 연결돼야 돌아서 오프라인에서 넣은 작업은 연결될 때까지 기다린다. 앱이 꺼져도 WorkManager 가 남겨 둔다.
 */
public class WorkManagerWorkQueue(context: Context, handler: WorkHandler) : WorkQueue {
    private val workManager = WorkManager.getInstance(context)

    init {
        QueueWorker.handler = handler
    }

    override suspend fun enqueue(id: String, payload: String) {
        if (workManager.getWorkInfosByTagFlow(id).first().any { !it.state.isFinished }) return
        val request = OneTimeWorkRequestBuilder<QueueWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, FIRST_BACKOFF.toJavaDuration())
            .setInputData(workDataOf(PAYLOAD to payload))
            .addTag(id)
            .build()
        // 앞 작업이 끝나야 다음 작업이 돈다. 앞 작업이 실패로 끝나면 뒤 작업도 실패하므로 QueueWorker 는 실패를 돌려주지 않는다.
        workManager.enqueueUniqueWork(CHAIN, ExistingWorkPolicy.APPEND_OR_REPLACE, request).await()
    }

    override suspend fun cancelAll() {
        workManager.cancelUniqueWork(CHAIN).await()
    }

    /** 아직 끝나지 않은 작업의 id. WorkManager 가 순서를 주지 않아 넣은 순서와 다를 수 있다. 디버깅 화면의 대기열 패널이 읽는다. */
    public suspend fun pending(): List<String> = workManager.getWorkInfosForUniqueWorkFlow(CHAIN).first()
        .filterNot { it.state.isFinished }
        // 태그에는 작업 id 말고 WorkManager 가 붙인 워커 클래스 이름도 들어 있다.
        .mapNotNull { info -> info.tags.firstOrNull { it != QueueWorker::class.java.name } }
}

private const val CHAIN = "work-queue"
internal const val PAYLOAD = "payload"

internal class QueueWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // ponytail: 핸들러를 전역 하나로 둔다. 앱 조립 전에 WorkManager 가 먼저 깨우면 다시 시도로 미룬다.
        val handler = handler ?: return Result.retry()
        return when (handler.handle(inputData.getString(PAYLOAD).orEmpty())) {
            WorkResult.Retry -> if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()

            // 버리는 작업도 성공으로 끝내야 사슬의 다음 작업이 돈다. 답변은 분석 전 상태로 남는다.
            WorkResult.Success, WorkResult.Failure -> Result.success()
        }
    }

    companion object {
        @Volatile
        var handler: WorkHandler? = null
    }
}

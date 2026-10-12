package io.github.nosorae.aiopic.apps.opic.androidapp

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.nosorae.aiopic.apps.opic.shared.AnalysisOutcome
import io.github.nosorae.aiopic.apps.opic.shared.AnalysisProgress
import io.github.nosorae.aiopic.apps.opic.shared.routeJson

/**
 * 분석 진행과 끝 알림(#229). 진행은 소리 없이 알림창에만 두고, 끝 알림은 소리와 함께 띄운다.
 * 알림 권한을 거절했으면 시스템이 띄우지 않을 뿐 분석은 그대로 돈다.
 */
internal object AnalysisNotifications {
    private const val PROGRESS = "analysis_progress"
    private const val DONE = "analysis_done"
    private const val DONE_ID = 2291

    /** 알림을 눌러 연 MainActivity 가 이동할 경로. */
    const val ROUTE = "route"

    fun createChannels(context: Context) = context.getSystemService(NotificationManager::class.java).createNotificationChannels(
        listOf(
            NotificationChannel(PROGRESS, "분석 진행", NotificationManager.IMPORTANCE_LOW),
            NotificationChannel(DONE, "리포트 준비", NotificationManager.IMPORTANCE_DEFAULT),
        ),
    )

    fun progress(context: Context, progress: AnalysisProgress): Notification = Notification.Builder(context, PROGRESS)
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setContentTitle(progress.title)
        .setContentText(progress.text)
        .setProgress(progress.total, progress.done, false)
        .setContentIntent(open(context, null))
        .setOngoing(true)
        .build()

    fun notify(context: Context, outcome: AnalysisOutcome) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        val notification = Notification.Builder(context, DONE)
            .setSmallIcon(if (outcome.failed) android.R.drawable.stat_notify_error else android.R.drawable.stat_sys_upload_done)
            .setContentTitle(outcome.title)
            .setContentText(outcome.text)
            .setContentIntent(open(context, outcome.routeJson))
            .setAutoCancel(true)
            .build()
        // ponytail: 끝 알림은 하나만 둔다. 새 결과가 앞 알림을 바꾼다. 리포트마다 따로 남겨야 하면 경로로 id 를 나눈다.
        manager.notify(DONE_ID, notification)
    }

    /** 앱의 화면이 지금 사용자에게 보이는가. 보이면 끝 알림을 띄우지 않는다. */
    fun appVisible(): Boolean = ActivityManager.RunningAppProcessInfo().also(ActivityManager::getMyMemoryState).importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND

    /** 진행 알림을 누르면 앱을 앞으로 가져온다. 끝 알림([route] 가 있다)은 앱을 새로 띄워 그 리포트를 연다. 앱이 화면에 없을 때만 띄우는 알림이라 앞 화면을 잃지 않는다. */
    private fun open(context: Context, route: String?): PendingIntent {
        val intent = route?.let {
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra(ROUTE, it)
        } ?: context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(context, if (route == null) 0 else 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

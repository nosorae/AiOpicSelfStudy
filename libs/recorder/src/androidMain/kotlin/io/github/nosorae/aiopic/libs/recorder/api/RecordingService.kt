package io.github.nosorae.aiopic.libs.recorder.api

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * 녹음하는 동안 떠 있는 마이크 포그라운드 서비스다(F3.5). 화면이 꺼지거나 다른 앱으로 가도 프로세스와 마이크 권한이 유지된다.
 * 녹음기 자체는 [AndroidRecorder] 가 같은 프로세스에서 들고 있고, 이 서비스는 떠 있기만 한다.
 */
internal class RecordingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "녹음", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("답변을 녹음하는 중")
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: RuntimeException) {
            // 마이크 권한이 없거나 앱이 화면에 없으면 마이크 서비스가 거절된다. 앱을 죽이지 않고 서비스만 내린다.
            // 녹음은 앱이 화면에 있는 동안 이어진다.
            stopSelf()
        }
        // 프로세스가 죽으면 녹음기도 없으니 서비스만 되살리지 않는다.
        return START_NOT_STICKY
    }

    private companion object {
        const val CHANNEL = "recording"
        const val NOTIFICATION_ID = 1
    }
}

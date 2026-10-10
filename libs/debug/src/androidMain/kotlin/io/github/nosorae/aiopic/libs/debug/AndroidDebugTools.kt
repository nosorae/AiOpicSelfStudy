package io.github.nosorae.aiopic.libs.debug

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.os.SystemClock
import java.io.File
import java.util.Locale

/**
 * Application.onCreate 에서 한 번 부른다. 스위치를 기기에 저장하고, 처리 안 된 예외가 나면 그 순간의 기록을 파일로 남긴다.
 * 디버깅 화면은 기기를 흔들거나 계속 떠 있는 알림을 눌러 연다. 알림 권한이 없으면 화면을 처음 열 때 묻는다.
 */
public fun DebugTools.install(application: Application, config: DebugConfig = DebugConfig()) {
    core.install(config, AndroidPlatform(application))
    showNotification(application)
    application.registerActivityLifecycleCallbacks(ShakeToOpen(application))
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        // 기록에 실패해도 원래 처리기(앱 종료, 크래시 보고)는 그대로 부른다.
        runCatching { core.recordCrash(error) }
        previous?.uncaughtException(thread, error)
    }
}

internal class AndroidPlatform(private val context: Context) : DebugPlatform {
    private val prefs = context.getSharedPreferences("debug_tools", Context.MODE_PRIVATE)
    private val crashDir = File(context.filesDir, "debug-crashes")

    override fun info(): List<Pair<String, String>> {
        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
        return listOf(
            "app" to "${context.packageName} ${pkg.versionName} (${pkg.longVersionCode})",
            "os" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "locale" to Locale.getDefault().toLanguageTag(),
            "free storage" to "${StatFs(context.filesDir.path).availableBytes / 1_000_000} MB",
        )
    }

    override fun readSwitch(key: String): Boolean? = if (prefs.contains(key)) prefs.getBoolean(key, false) else null

    override fun writeSwitch(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
    }

    override fun crashNames(): List<String> = crashDir.list()?.toList().orEmpty()

    override fun readCrash(name: String): String = File(crashDir, name).readText()

    override fun writeCrash(name: String, text: String) {
        crashDir.mkdirs()
        File(crashDir, name).writeText(text)
    }

    override fun deleteCrash(name: String) {
        File(crashDir, name).delete()
    }

    override fun open() {
        context.startActivity(debugIntent(context))
    }
}

private fun debugIntent(context: Context) = Intent(context, DebugActivity::class.java)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

private const val CHANNEL = "debug_tools"

/** 누르면 디버깅 화면이 열리는 알림. 밀어서 지우지 못하게 ongoing 으로 둔다. 권한이 없으면 아무 일도 안 한다. */
internal fun showNotification(context: Context) {
    if (Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(NotificationChannel(CHANNEL, "Debug tools", NotificationManager.IMPORTANCE_LOW))
    val open = PendingIntent.getActivity(context, 0, debugIntent(context), PendingIntent.FLAG_IMMUTABLE)
    val notification = Notification.Builder(context, CHANNEL)
        .setSmallIcon(android.R.drawable.ic_menu_info_details)
        .setContentTitle("Debug tools")
        .setContentText("Tap to open logs, network and switches")
        .setContentIntent(open)
        .setOngoing(true)
        .build()
    manager.notify(CHANNEL.hashCode(), notification)
}

/** 앱 화면이 앞에 있을 때만 가속도 센서를 듣는다. 뒤로 가면 끊어서 배터리를 쓰지 않는다. */
private class ShakeToOpen(private val context: Context) :
    Application.ActivityLifecycleCallbacks,
    SensorEventListener {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val detector = ShakeDetector()

    override fun onActivityResumed(activity: Activity) {
        sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onActivityPaused(activity: Activity) {
        sensors?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        if (detector.isShake(x, y, z, SystemClock.elapsedRealtime())) context.startActivity(debugIntent(context))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}

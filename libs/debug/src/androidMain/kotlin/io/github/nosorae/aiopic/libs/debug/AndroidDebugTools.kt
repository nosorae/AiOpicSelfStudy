package io.github.nosorae.aiopic.libs.debug

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.StatFs
import java.io.File
import java.util.Locale

/** Application.onCreate 에서 한 번 부른다. 스위치를 기기에 저장하고, 처리 안 된 예외가 나면 그 순간의 기록을 파일로 남긴다. */
public fun DebugTools.install(application: Application, config: DebugConfig = DebugConfig()) {
    core.install(config, AndroidPlatform(application))
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
}

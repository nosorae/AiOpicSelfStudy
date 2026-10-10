package io.github.nosorae.aiopic.libs.debug

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File

/** 디버깅 화면. 앱의 Activity 와 따로 떠서 앱 화면 구조를 건드리지 않는다. */
internal class DebugActivity : ComponentActivity() {
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showNotification(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DebugApp(DebugTools.core, onShare = ::share, onClose = ::finish) }
        // 알림으로 다시 열 수 있게 처음 열 때 한 번 권한을 묻는다. 거절하면 흔들기와 앱의 메뉴로만 연다.
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun share(report: String) {
        val file = File(cacheDir, "debug-tools/debug-report.txt").apply {
            parentFile?.mkdirs()
            writeText(report)
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.debugtools", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, "Debug report"))
    }
}

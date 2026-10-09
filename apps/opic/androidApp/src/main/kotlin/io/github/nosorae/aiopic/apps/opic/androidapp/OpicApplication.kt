package io.github.nosorae.aiopic.apps.opic.androidapp

import android.app.Application
import io.github.nosorae.aiopic.apps.opic.shared.AppGraph
import io.github.nosorae.aiopic.apps.opic.shared.AppSettings
import io.github.nosorae.aiopic.apps.opic.shared.createAppGraph
import io.github.nosorae.aiopic.apps.opic.shared.data.opicDatabase
import io.github.nosorae.aiopic.apps.opic.shared.data.recordingsRoot
import io.github.nosorae.aiopic.libs.keyvault.api.KeystoreKeyVault
import io.github.nosorae.aiopic.libs.recorder.api.AndroidRecorder
import io.github.nosorae.aiopic.libs.tts.AndroidTts
import io.github.nosorae.aiopic.libs.workqueue.api.WorkManagerWorkQueue

/**
 * 앱 프로세스에 그래프 하나를 둔다. 화면이 없을 때 WorkManager 가 프로세스를 깨워도 분석 대기열이 처리기를 찾게
 * Activity 가 아니라 여기서 만든다(F5.1).
 */
class OpicApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        graph = createAppGraph(
            settings = AppSettings(read = { prefs.getString(it, null) }, write = { key, value -> prefs.edit().putString(key, value).apply() }),
            keyVault = KeystoreKeyVault(this),
            database = opicDatabase(this),
            recordingsRoot = recordingsRoot(this),
            recorder = AndroidRecorder(this),
            tts = AndroidTts(this),
            // 대기열이 그래프보다 먼저 만들어지므로 처리기는 꺼낼 때 그래프에서 찾는다.
            workQueue = WorkManagerWorkQueue(this) { graph.analysisHandler.handle(it) },
        )
    }
}

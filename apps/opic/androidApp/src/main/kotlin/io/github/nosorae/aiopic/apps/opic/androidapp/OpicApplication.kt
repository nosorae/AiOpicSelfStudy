package io.github.nosorae.aiopic.apps.opic.androidapp

import android.app.Application
import android.media.AudioManager
import io.github.nosorae.aiopic.apps.opic.shared.AppGraph
import io.github.nosorae.aiopic.apps.opic.shared.AppSettings
import io.github.nosorae.aiopic.apps.opic.shared.createAppGraph
import io.github.nosorae.aiopic.apps.opic.shared.data.opicDatabase
import io.github.nosorae.aiopic.apps.opic.shared.data.recordingsRoot
import io.github.nosorae.aiopic.apps.opic.shared.debugGeminiClient
import io.github.nosorae.aiopic.apps.opic.shared.registerDebugPanels
import io.github.nosorae.aiopic.apps.opic.shared.withDebugTools
import io.github.nosorae.aiopic.libs.debug.DebugTools
import io.github.nosorae.aiopic.libs.debug.install
import io.github.nosorae.aiopic.libs.keyvault.api.KeystoreKeyVault
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.github.nosorae.aiopic.libs.recorder.api.AndroidRecorder
import io.github.nosorae.aiopic.libs.tts.AndroidTts
import io.github.nosorae.aiopic.libs.workqueue.api.WorkManagerWorkQueue

/**
 * 앱 프로세스에 그래프 하나를 둔다. 화면이 없을 때 WorkManager 가 프로세스를 깨워도 분석 대기열이 처리기를 찾게
 * Activity 가 아니라 여기서 만든다(F5.1).
 */
open class OpicApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        // 그래프가 스위치를 읽고 클라이언트를 만들기 전에 설치한다. 릴리스 APK 는 빈 모듈이라 아무 일도 안 한다.
        DebugTools.install(this)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        // 대기열이 그래프보다 먼저 만들어지므로 처리기는 꺼낼 때 그래프에서 찾는다.
        val workQueue = WorkManagerWorkQueue(this) { graph.analysisHandler.handle(it) }
        graph = createAppGraph(
            settings = AppSettings(read = { prefs.getString(it, null) }, write = { key, value -> prefs.edit().putString(key, value).apply() }),
            keyVault = KeystoreKeyVault(this),
            database = opicDatabase(this),
            recordingsRoot = recordingsRoot(this),
            recorder = AndroidRecorder(this).withDebugTools(),
            tts = AndroidTts(this),
            workQueue = workQueue,
            gemini = gemini(),
        )
        graph.registerDebugPanels(queue = workQueue::pending, audio = ::audioRows)
    }

    /** 시나리오 테스트가 저장해 둔 응답을 돌려주는 클라이언트로 바꾼다. */
    protected open fun gemini(): GeminiClient = debugGeminiClient()

    private fun audioRows(): List<Pair<String, String>> {
        val audio = getSystemService(AudioManager::class.java)
        return listOf(
            "mode" to audio.mode.toString(),
            "music active" to audio.isMusicActive.toString(),
            "outputs" to audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).joinToString { it.type.toString() },
            "inputs" to audio.getDevices(AudioManager.GET_DEVICES_INPUTS).joinToString { it.type.toString() },
        )
    }
}

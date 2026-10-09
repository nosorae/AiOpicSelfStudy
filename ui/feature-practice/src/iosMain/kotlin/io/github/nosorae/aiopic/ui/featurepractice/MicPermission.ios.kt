package io.github.nosorae.aiopic.ui.featurepractice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import kotlin.coroutines.resume

@Composable
internal actual fun rememberMicPermission(): suspend () -> Boolean = remember {
    suspend {
        val app = AVAudioApplication.sharedInstance
        if (app.recordPermission == AVAudioApplicationRecordPermissionGranted) {
            true
        } else {
            suspendCancellableCoroutine { continuation ->
                AVAudioApplication.requestRecordPermissionWithCompletionHandler { granted -> continuation.resume(granted) }
            }
        }
    }
}

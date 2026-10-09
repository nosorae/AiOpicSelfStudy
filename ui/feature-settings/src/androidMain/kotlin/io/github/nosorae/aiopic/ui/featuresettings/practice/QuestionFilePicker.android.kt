package io.github.nosorae.aiopic.ui.featuresettings.practice

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/** 파일 앱마다 JSON 을 다른 형식으로 알려서 글자 파일과 형식을 모르는 파일도 고를 수 있게 둔다. 내용은 불러올 때 검사한다. */
private val JsonTypes = arrayOf("application/json", "text/plain", "application/octet-stream")

@Composable
internal actual fun rememberQuestionFilePicker(onPick: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnPick by rememberUpdatedState(onPick)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // ponytail: 문항 파일은 몇십 KB 라 메인 스레드에서 읽는다. MB 단위 파일을 받게 되면 IO 디스패처로 옮긴다.
        val text = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
        currentOnPick(text)
    }
    return { launcher.launch(JsonTypes) }
}

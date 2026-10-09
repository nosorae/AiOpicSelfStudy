package io.github.nosorae.aiopic.ui.featuresettings.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberQuestionFilePicker(onPick: (String?) -> Unit): () -> Unit {
    val currentOnPick by rememberUpdatedState(onPick)
    val controller = LocalUIViewController.current
    // 선택기는 delegate 를 약하게 잡아서 화면이 delegate 를 들고 있는다.
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                currentOnPick(NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null))
            }
        }
    }
    return remember(controller) {
        {
            // 앱 폴더로 복사해 받으므로 보안 범위 접근을 열고 닫을 필요가 없다.
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON), asCopy = true)
            picker.delegate = delegate
            controller.presentViewController(picker, animated = true, completion = null)
        }
    }
}

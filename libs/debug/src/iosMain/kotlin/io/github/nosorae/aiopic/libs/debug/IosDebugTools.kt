package io.github.nosorae.aiopic.libs.debug

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.writeToFile
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController
import platform.UIKit.restorationIdentifier
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.uname
import platform.posix.utsname
import kotlin.experimental.ExperimentalNativeApi

/**
 * 앱 시작 때 한 번 부른다. 스위치를 기기에 저장하고, Kotlin 에서 처리 안 된 예외가 나면 그 순간의 기록을 파일로 남긴다.
 * 네이티브 신호로 죽는 크래시(EXC_BAD_ACCESS 등)는 잡지 않는다.
 */
@OptIn(ExperimentalNativeApi::class)
public fun DebugTools.install(config: DebugConfig = DebugConfig()) {
    core.install(config, IosPlatform(crashDir()))
    val previous = getUnhandledExceptionHook()
    setUnhandledExceptionHook { error ->
        // 기록에 실패해도 원래 처리기는 그대로 부른다.
        runCatching { core.recordCrash(error) }
        previous?.invoke(error)
    }
}

/**
 * 디버깅 화면. 앱이 원하는 곳에서 직접 띄울 때 쓴다. 닫기를 누르면 스스로 dismiss 한다.
 * 흔들기는 앱 Swift 쪽 `UIWindow.motionEnded` 에서 [DebugTools.open] 을 부른다(README).
 */
public fun DebugTools.viewController(): UIViewController {
    lateinit var controller: UIViewController
    controller = ComposeUIViewController {
        DebugApp(
            core,
            onShare = { share(controller, it) },
            onClose = { controller.dismissViewControllerAnimated(true, null) },
            reduceMotion = UIAccessibilityIsReduceMotionEnabled(),
        )
    }
    return controller
}

/** 보고서를 임시 파일로 써서 공유 시트에 넘긴다. 보고서가 수 MB 가 될 수 있어 문자열 대신 파일을 넘긴다. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun share(from: UIViewController, report: String) {
    val path = NSTemporaryDirectory() + "debug-report.txt"
    @Suppress("CAST_NEVER_SUCCEEDS")
    (report as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    val sheet = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
    sheet.popoverPresentationController?.sourceView = from.view
    from.presentViewController(sheet, animated = true, completion = null)
}

/** 맨 위에 떠 있는 화면 위에 디버깅 화면을 띄운다. 이미 떠 있으면 다시 띄우지 않는다. */
private fun presentOnTop() {
    val root = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .flatMap { it.windows.filterIsInstance<UIWindow>() }
        .firstOrNull { it.isKeyWindow() }?.rootViewController ?: return
    var top: UIViewController = root
    while (true) top = top.presentedViewController ?: break
    if (top.restorationIdentifier == RESTORATION_ID) return
    val debug = DebugTools.viewController().apply { restorationIdentifier = RESTORATION_ID }
    top.presentViewController(debug, animated = true, completion = null)
}

private const val RESTORATION_ID = "debug-tools"

private fun crashDir(): String = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String + "/debug-crashes"

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosPlatform(private val crashDir: String) : DebugPlatform {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val files = NSFileManager.defaultManager

    override fun info(): List<Pair<String, String>> {
        val bundle = NSBundle.mainBundle
        val device = UIDevice.currentDevice
        val free = files.attributesOfFileSystemForPath(NSHomeDirectory(), null)?.get(NSFileSystemFreeSize) as? NSNumber
        return listOf(
            "app" to "${bundle.bundleIdentifier} ${bundle.objectForInfoDictionaryKey("CFBundleShortVersionString")} " +
                "(${bundle.objectForInfoDictionaryKey("CFBundleVersion")})",
            "os" to "${device.systemName} ${device.systemVersion}",
            "device" to memScoped { alloc<utsname>().also { uname(it.ptr) }.machine.toKString() },
            "locale" to NSLocale.currentLocale.localeIdentifier,
            "free storage" to "${(free?.longLongValue ?: 0L) / 1_000_000} MB",
        )
    }

    override fun readSwitch(key: String): Boolean? = if (defaults.objectForKey(PREFIX + key) != null) defaults.boolForKey(PREFIX + key) else null

    override fun writeSwitch(key: String, enabled: Boolean) {
        defaults.setBool(enabled, PREFIX + key)
    }

    override fun crashNames(): List<String> = files.contentsOfDirectoryAtPath(crashDir, null).orEmpty().map { it as String }

    override fun readCrash(name: String): String = NSString.create(contentsOfFile = "$crashDir/$name", encoding = NSUTF8StringEncoding, error = null)?.toString().orEmpty()

    override fun writeCrash(name: String, text: String) {
        files.createDirectoryAtPath(crashDir, withIntermediateDirectories = true, attributes = null, error = null)
        @Suppress("CAST_NEVER_SUCCEEDS")
        (text as NSString).writeToFile("$crashDir/$name", atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    override fun deleteCrash(name: String) {
        files.removeItemAtPath("$crashDir/$name", null)
    }

    override fun open() {
        // 흔들기와 앱 코드가 어느 스레드에서 부르든 UIKit 은 메인 스레드에서만 만진다.
        dispatch_async(dispatch_get_main_queue()) { presentOnTop() }
    }

    private companion object {
        const val PREFIX = "debug_tools."
    }
}

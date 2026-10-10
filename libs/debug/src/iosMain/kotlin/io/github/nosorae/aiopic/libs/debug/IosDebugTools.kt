package io.github.nosorae.aiopic.libs.debug

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
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.writeToFile
import platform.UIKit.UIDevice
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

    private companion object {
        const val PREFIX = "debug_tools."
    }
}

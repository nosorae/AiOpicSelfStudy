package io.github.nosorae.aiopic.apps.opic.shared

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

internal actual fun readFile(path: String): ByteArray = checkNotNull(readFileOrNull(path)) { "녹음 파일을 못 읽는다: $path" }

@OptIn(ExperimentalForeignApi::class)
internal actual fun readFileOrNull(path: String): ByteArray? {
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    return ByteArray(data.length.toInt()).apply {
        if (isNotEmpty()) usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual fun writeFile(path: String, bytes: ByteArray) {
    NSFileManager.defaultManager.createDirectoryAtPath(path.substringBeforeLast('/'), withIntermediateDirectories = true, attributes = null, error = null)
    if (bytes.isEmpty()) return
    // atomically: 다 쓴 뒤에 이름을 바꿔서, 쓰는 중에 앱이 꺼져도 반쯤 쓴 파일을 틀지 않는다.
    bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }.writeToFile(path, atomically = true)
}

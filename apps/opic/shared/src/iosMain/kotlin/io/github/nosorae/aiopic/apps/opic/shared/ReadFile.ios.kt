package io.github.nosorae.aiopic.apps.opic.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal actual fun readFile(path: String): ByteArray {
    val data = checkNotNull(NSData.dataWithContentsOfFile(path)) { "녹음 파일을 못 읽는다: $path" }
    return ByteArray(data.length.toInt()).apply {
        if (isNotEmpty()) usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    }
}

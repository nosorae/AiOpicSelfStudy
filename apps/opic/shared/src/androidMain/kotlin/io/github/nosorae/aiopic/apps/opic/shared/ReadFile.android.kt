package io.github.nosorae.aiopic.apps.opic.shared

import java.io.File

internal actual fun readFile(path: String): ByteArray = File(path).readBytes()

internal actual fun readFileOrNull(path: String): ByteArray? = File(path).takeIf { it.isFile }?.readBytes()

internal actual fun writeFile(path: String, bytes: ByteArray) {
    val file = File(path)
    file.parentFile?.mkdirs()
    // 다 쓴 뒤에 이름을 바꿔서, 쓰는 중에 앱이 꺼져도 반쯤 쓴 파일을 틀지 않는다.
    val partial = File("$path.partial")
    partial.writeBytes(bytes)
    partial.renameTo(file)
}

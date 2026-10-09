package io.github.nosorae.aiopic.apps.opic.shared

import java.io.File

internal actual fun readFile(path: String): ByteArray = File(path).readBytes()
